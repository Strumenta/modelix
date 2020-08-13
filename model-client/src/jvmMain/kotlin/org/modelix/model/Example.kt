package org.modelix.model

import org.modelix.model.api.*
import org.modelix.model.client.ReplicatedTree
import org.modelix.model.client.RestWebModelClient
import org.modelix.model.lazy.CLTree
import org.modelix.model.lazy.IConceptSerializer
import org.modelix.model.lazy.TreeId
import java.util.*

object MyDummyConcept : IConcept {
    override fun isSubconceptOf(superConcept: IConcept?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isExactly(concept: IConcept?): Boolean {
        TODO("Not yet implemented")
    }

    override val properties: Iterable<IProperty>
        get() = TODO("Not yet implemented")
    override val childLinks: Iterable<IChildLink>
        get() = TODO("Not yet implemented")
    override val referenceLinks: Iterable<IReferenceLink>
        get() = TODO("Not yet implemented")

    override fun getProperty(name: String): IProperty {
        TODO("Not yet implemented")
    }

    override fun getChildLink(name: String): IChildLink {
        TODO("Not yet implemented")
    }

    override fun getReferenceLink(name: String): IReferenceLink {
        TODO("Not yet implemented")
    }

}

fun main(args: Array<String>) {
    val serverUrl = "http://localhost:28101/"
    val client = RestWebModelClient(serverUrl)
    val treeId: TreeId = TreeId("76606689-9fcb-451a-98f9-1aee3f45cae6")
    println("Tree Id $treeId")
    val branchName = "master"
    val user : () -> String = { "demo" }
    val tree = object : ReplicatedTree(client, treeId, branchName, user) {

    }

    IConceptSerializer.register(object : IConceptSerializer {
        override fun serialize(concept: IConcept): String? {
            return "DUMMY_CONCEPT"
        }

        override fun deserialize(serialized: String): IConcept? {
            return if (serialized == "DUMMY_CONCEPT") MyDummyConcept else null
        }
    })

//    val node = tree.createElement(null)
//    client.asyncSto
    val res = tree.branch.computeWrite {
        tree.branch.writeTransaction.run {
            val concept = MyDummyConcept
            this.addNewChild(ITree.ROOT_ID, "child", 0, concept)
        }
    }
    println("res $res")
}