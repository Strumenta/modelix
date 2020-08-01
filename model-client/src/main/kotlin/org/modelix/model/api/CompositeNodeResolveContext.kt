package org.modelix.model.api

import org.modelix.model.util.StreamUtils
import java.util.*
import java.util.stream.Collectors

class CompositeNodeResolveContext(contexts: Iterable<INodeResolveContext>) : INodeResolveContext {
    private val contexts: List<INodeResolveContext>

    constructor(vararg contexts: INodeResolveContext) : this(Arrays.asList(*contexts)) {}

    override fun resolve(ref: INodeReference?): INode? {
        return contexts.stream().map { it: INodeResolveContext -> it.resolve(ref) }.filter { obj: INode? -> Objects.nonNull(obj) }.findFirst().orElse(null)
    }

    companion object {
        fun withAdditionalContext(context: INodeResolveContext, runnable: Runnable?) {
            val activeContext: INodeResolveContext? = INodeResolveContext.Companion.CONTEXT_VALUE.getValue()
            if (activeContext == null) {
                INodeResolveContext.Companion.CONTEXT_VALUE.runWith(context, runnable!!)
            } else {
                INodeResolveContext.Companion.CONTEXT_VALUE.runWith(CompositeNodeResolveContext(context, activeContext), runnable!!)
            }
        }
    }

    init {
        this.contexts = contexts.toList()
    }
}