package org.modelix.model.api

expect interface ITransaction
expect interface IReadTransaction : ITransaction
expect interface IWriteTransaction : ITransaction
expect interface ITree
expect interface INodeResolveContext