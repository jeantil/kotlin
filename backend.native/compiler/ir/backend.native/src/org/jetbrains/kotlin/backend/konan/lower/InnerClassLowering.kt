package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.lower.callsSuper
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.utils.addToStdlib.singletonList

internal class InnerClassLowering(val context: Context) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        InnerClassTransformer(irClass).lowerInnerClass()
    }

    private inner class InnerClassTransformer(val irClass: IrClass) {
        val classDescriptor = irClass.descriptor

        lateinit var outerThisFieldDescriptor: PropertyDescriptor

        fun lowerInnerClass() {
            if (!irClass.descriptor.isInner) return

            createOuterThisField()
            lowerOuterThisReferences()
            lowerConstructors()
        }

        object DECLARATION_ORIGIN_FIELD_FOR_OUTER_THIS :
                IrDeclarationOriginImpl("FIELD_FOR_OUTER_THIS")

        private fun createOuterThisField() {
            outerThisFieldDescriptor = context.specialDescriptorsFactory.getOuterThisFieldDescriptor(classDescriptor)

            irClass.declarations.add(IrFieldImpl(
                    irClass.startOffset, irClass.endOffset,
                    DECLARATION_ORIGIN_FIELD_FOR_OUTER_THIS,
                    outerThisFieldDescriptor
            ))
        }

        private fun lowerConstructors() {
            irClass.declarations.transformFlat { irMember ->
                if (irMember is IrConstructor)
                    lowerConstructor(irMember).singletonList()
                else
                    null
            }
        }

        private fun lowerConstructor(irConstructor: IrConstructor): IrConstructor {
            if (irConstructor.callsSuper()) {
                // Initializing constructor: initialize 'this.this$0' with '$outer'.
                val blockBody = irConstructor.body as? IrBlockBody
                        ?: throw AssertionError("Unexpected constructor body: ${irConstructor.body}")
                val startOffset = irConstructor.startOffset
                val endOffset = irConstructor.endOffset
                blockBody.statements.add(
                        0,
                        IrSetFieldImpl(
                                startOffset, endOffset, outerThisFieldDescriptor,
                                IrGetValueImpl(startOffset, endOffset, classDescriptor.thisAsReceiverParameter),
                                IrGetValueImpl(startOffset, endOffset, irConstructor.descriptor.dispatchReceiverParameter!!)
                        )
                )
            }

            return irConstructor
        }

        private fun lowerOuterThisReferences() {
            irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
                private fun <E> MutableList<E>.push(element: E) = this.add(element)

                private fun <E> MutableList<E>.pop() = this.removeAt(size - 1)

                private fun <E> MutableList<E>.peek(): E? = if (size == 0) null else this[size - 1]

                private val functionsStack = mutableListOf<FunctionDescriptor>()

                override fun visitFunction(declaration: IrFunction): IrStatement {
                    functionsStack.push(declaration.descriptor)
                    val result = super.visitFunction(declaration)
                    functionsStack.pop()
                    return result
                }

                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    expression.transformChildrenVoid(this)

                    val implicitThisClass = expression.descriptor.getClassDescriptorForImplicitThis() ?:
                            return expression

                    if (implicitThisClass == classDescriptor) return expression

                    val constructorDescriptor = functionsStack.peek()!! as? ConstructorDescriptor

                    val startOffset = expression.startOffset
                    val endOffset = expression.endOffset
                    val origin = expression.origin

                    var irThis: IrExpression
                    var innerClass: ClassDescriptor
                    if (constructorDescriptor == null) {
                        innerClass = classDescriptor
                        irThis = IrGetValueImpl(startOffset, endOffset, classDescriptor.thisAsReceiverParameter, origin)
                    } else {
                        // For constructor we have outer class as dispatchReceiverParameter.
                        innerClass = DescriptorUtils.getContainingClass(classDescriptor) ?:
                                throw AssertionError("No containing class for inner class $classDescriptor")
                        irThis = IrGetValueImpl(startOffset, endOffset, constructorDescriptor.dispatchReceiverParameter!!, origin)
                    }

                    while (innerClass != implicitThisClass) {
                        if (!innerClass.isInner) {
                            // Captured 'this' unrelated to inner classes nesting hierarchy, leave it as is -
                            // should be transformed by closures conversion.
                            return expression
                        }

                        val outerThisField = context.specialDescriptorsFactory.getOuterThisFieldDescriptor(innerClass)
                        irThis = IrGetFieldImpl(startOffset, endOffset, outerThisField, irThis, origin)

                        val outer = innerClass.containingDeclaration
                        innerClass = outer as? ClassDescriptor ?:
                                throw AssertionError("Unexpected containing declaration for inner class $innerClass: $outer")
                    }

                    return irThis
                }
            })
        }

        private fun ValueDescriptor.getClassDescriptorForImplicitThis(): ClassDescriptor? {
            if (this is ReceiverParameterDescriptor) {
                val receiverValue = value
                if (receiverValue is ImplicitClassReceiver) {
                    return receiverValue.classDescriptor
                }
            }
            return null
        }
    }
}

