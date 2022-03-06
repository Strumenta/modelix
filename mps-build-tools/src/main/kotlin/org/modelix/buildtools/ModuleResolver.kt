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
package org.modelix.buildtools

class ModuleResolver(val availableModules: FoundModules, val ignoredModules: Set<ModuleId>) {
    fun resolveModule(dep: ModuleDependency, usedBy: FoundModule): FoundModule? {
        return resolveModule(ModuleIdAndName(dep.id, dep.moduleName), usedBy)
    }

    fun resolveModule(dep: ModuleIdAndName, usedBy: FoundModule): FoundModule? {
        val resolved = availableModules.getModules()[dep.id]
        if (resolved == null && !ignoredModules.contains(dep.id)) {
            throw RuntimeException("Dependency $dep not found (used by ${usedBy.moduleId}(${usedBy.name}) in ${usedBy.owner.path.getLocalAbsolutePath()} )")
        }
        return resolved
    }
}