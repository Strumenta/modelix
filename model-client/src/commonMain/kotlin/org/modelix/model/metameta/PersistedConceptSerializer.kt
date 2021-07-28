/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modelix.model.metameta

import org.modelix.model.api.IConcept
import org.modelix.model.api.ITree
import org.modelix.model.lazy.IConceptReferenceSerializer
import org.modelix.model.persistent.SerializationUtil

class PersistedConceptSerializer : IConceptReferenceSerializer {
    override fun serialize(concept: IConcept): String? {
        if (concept is PersistedConcept) {
            return SerializationUtil.longToHex(concept.id)
        }
        return null
    }

    override fun deserialize(serialized: String, tree: ITree?): IConcept? {
        if (serialized matches Regex("[a-fA-Z0-9]+")) {
            var uid: String? = null
            val conceptNodeId = SerializationUtil.longFromHex(serialized)
            if (tree != null && tree.containsNode(conceptNodeId)) {
                uid = tree.getProperty(conceptNodeId, MetaMetaLanguage.property_IHasUID_uid.name)
            }
            return PersistedConcept(conceptNodeId, uid)
        }
        return null
    }
}
