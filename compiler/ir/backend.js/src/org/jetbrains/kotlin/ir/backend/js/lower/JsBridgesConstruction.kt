/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.utils.eraseGenerics
import org.jetbrains.kotlin.ir.backend.js.utils.hasStableJsName
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name

class JsBridgesConstruction(context: JsIrBackendContext) : BridgesConstruction<JsIrBackendContext>(context) {
    override fun getFunctionSignature(function: IrSimpleFunction): JsSignature =
        function.jsSignature(context.irBuiltIns)

    override fun getBridgeOrigin(bridge: IrSimpleFunction): IrDeclarationOrigin =
        if (bridge.hasStableJsName(context))
            JsLoweredDeclarationOrigin.BRIDGE_WITH_STABLE_NAME
        else
            JsLoweredDeclarationOrigin.BRIDGE_WITHOUT_STABLE_NAME
}

data class JsSignature(
    val name: Name,
    val extensionReceiverType: IrType?,
    val valueParametersType: List<IrType>,
) {
    override fun toString(): String {
        val er = extensionReceiverType?.let { "(er: ${it.render()}) " } ?: ""
        val parameters = valueParametersType.joinToString(", ") { it.render() }
        return "[$er$name($parameters)]"
    }
}

fun IrSimpleFunction.jsSignature(irBuiltIns: IrBuiltIns): JsSignature =
    JsSignature(
        name,
        extensionReceiverParameter?.type?.eraseGenerics(irBuiltIns),
        valueParameters.map { it.type.eraseGenerics(irBuiltIns) },
    )