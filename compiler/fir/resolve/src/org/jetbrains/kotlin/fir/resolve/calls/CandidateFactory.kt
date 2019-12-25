/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.returnExpressions
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzer
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind

class CandidateFactory(
    val bodyResolveComponents: BodyResolveComponents,
    private val callInfo: CallInfo
) {

    private val baseSystem: ConstraintStorage

    init {
        val system = bodyResolveComponents.inferenceComponents.createConstraintSystem()
        callInfo.arguments.forEach {
            system.addSubsystemFromExpression(it)
        }
        baseSystem = system.asReadOnlyStorage()
    }

    fun createCandidate(
        symbol: AbstractFirBasedSymbol<*>,
        explicitReceiverKind: ExplicitReceiverKind,
        dispatchReceiverValue: ReceiverValue? = null,
        implicitExtensionReceiverValue: ImplicitReceiverValue<*>? = null,
        builtInExtensionFunctionReceiverValue: ReceiverValue? = null
    ): Candidate {
        return Candidate(
            symbol, dispatchReceiverValue, implicitExtensionReceiverValue,
            explicitReceiverKind, bodyResolveComponents, baseSystem,
            builtInExtensionFunctionReceiverValue?.receiverExpression?.let {
                callInfo.withReceiverAsArgument(it)
            } ?: callInfo
        )
    }
}

fun PostponedArgumentsAnalyzer.Context.addSubsystemFromExpression(statement: FirStatement) {
    when (statement) {
        is FirFunctionCall, is FirQualifiedAccessExpression, is FirWhenExpression, is FirTryExpression, is FirCheckNotNullCall, is FirCallableReferenceAccess ->
            (statement as FirResolvable).candidate()?.let { addOtherSystem(it.system.asReadOnlyStorage()) }
        is FirWrappedArgumentExpression -> addSubsystemFromExpression(statement.expression)
        is FirBlock -> statement.returnExpressions().forEach { addSubsystemFromExpression(it) }
        else -> {}
    }
}

internal fun FirResolvable.candidate(): Candidate? {
    return when (val callee = this.calleeReference) {
        is FirNamedReferenceWithCandidate -> return callee.candidate
        else -> null
    }
}
