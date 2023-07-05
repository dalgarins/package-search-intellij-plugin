// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.packageSearch.mppDependencyUpdater

import com.android.tools.idea.gradle.dsl.api.GradleBuildModel
import com.android.tools.idea.gradle.dsl.api.ProjectBuildModel
import com.android.tools.idea.gradle.dsl.api.dependencies.ArtifactDependencyModel
import com.android.tools.idea.gradle.dsl.api.dependencies.ArtifactDependencySpec
import com.android.tools.idea.gradle.dsl.api.dependencies.DependenciesModel
import com.android.tools.idea.gradle.dsl.api.ext.PropertyType
import com.android.tools.idea.gradle.dsl.api.ext.ResolvedPropertyModel
import com.android.tools.idea.gradle.dsl.api.util.GradleDslModel
import com.intellij.openapi.application.readAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.util.alsoIfNull
import com.intellij.packageSearch.mppDependencyUpdater.dsl.models.KotlinDslModel
import com.intellij.packageSearch.mppDependencyUpdater.dsl.models.SourceSetModel

private val LOG = logger<MppDependencyModificator>()

object MppDependencyModificator {
  suspend fun isAvailable(module: Module): Boolean =
    // TODO check if the plugin is available
    readFromKotlinModel(module) { it.isAvailable } ?: false

  suspend fun sourceSets(module: Module): List<SourceSetModel>? =
    readFromKotlinModel(module) { it.sourceSets()?.values?.toList() }

  suspend fun dependenciesBySourceSet(module: Module): Map<String, DependenciesModel?>? =
    readFromKotlinModel(module) { kotlinDslModel ->
      kotlinDslModel.sourceSets()?.mapValues {
        it.value.dependencies()
      }
    }

  suspend fun addDependency(
    module: Module,
    sourceSet: String,
    descriptor: MppDependency,
    createIfMissing: Boolean = true,
  ) = addDependency(module, MppModifierAddData(sourceSet, descriptor), createIfMissing)

  suspend fun addDependency(
    module: Module,
    data: MppModifierAddData,
    createIfMissing: Boolean = true,
  ) = addDependencies(module, listOf(data), createIfMissing)

  suspend fun addDependencies(
    module: Module,
    data: List<MppModifierAddData>,
    createIfMissing: Boolean = true,
  ) {
    modifyKotlinModel(module) { kotlinModel ->
      data.map { (sourceSet, mppDependency) ->
        when (mppDependency) {
          is MppDependency.Maven -> addToKotlinModel(kotlinModel, sourceSet, mppDependency, createIfMissing)
          is MppDependency.Npm -> TODO()
          is MppDependency.Cocoapods -> TODO()
        }
      }
    }
  }

  private fun addToKotlinModel(
    model: KotlinDslModel,
    sourceSet: String,
    mppDependency: MppDependency.Maven,
    createIfMissing: Boolean,
    ) {
    val artifactSpec = mppDependency.artifactDependencySpec()

    val sourceSetModel = model.getOrCreateSourceSet(sourceSet, createIfMissing)
                      .alsoIfNull { LOG.warn("Requested source set ${sourceSet} not found") }
                    ?: return

    sourceSetModel.getOrCreateDependenciesBlock(createIfMissing)?.addArtifact(mppDependency.configuration, artifactSpec)
      .alsoIfNull { LOG.warn("Could not find dependencies block in source set ${sourceSet}") }
    ?: return
  }

  suspend fun removeDependency(
    module: Module,
    sourceSet: String,
    descriptor: MppDependency,
  ) = removeDependency(module, MppModifierRemoveData(sourceSet, descriptor))

  suspend fun removeDependency(
    module: Module,
    data: MppModifierRemoveData,
  ) = removeDependencies(module, listOf(data))

  suspend fun removeDependencies(
    module: Module,
    data: List<MppModifierRemoveData>,
  ) {
    modifyKotlinModel(module) { kotlinModel ->
      data.map { (sourceSet, mppDependency) ->
        when (mppDependency) {
          is MppDependency.Maven -> removeFromKotlinModel(kotlinModel, sourceSet, mppDependency)
          is MppDependency.Npm -> TODO()
          is MppDependency.Cocoapods -> TODO()
        }
      }
    }
  }

  private fun removeFromKotlinModel(model: KotlinDslModel, sourceSet: String, dependency: MppDependency.Maven) {
    val sourceSetModel = model.sourceSets()?.get(sourceSet)
                      .alsoIfNull { LOG.warn("Requested source set ${sourceSet} not found") }
                    ?: return

    val dependencies = sourceSetModel.dependencies()
                         .alsoIfNull { LOG.warn("Could not find dependencies block in source set ${sourceSet}") }
                       ?: return

    dependencies.findDependency(dependency)?.also { dependencies.remove(it) }
      .alsoIfNull { LOG.warn("Could not find dependency $dependency") }
    ?: return
  }

  suspend fun updateDependency(
    module: Module,
    sourceSet: String,
    oldDescriptor: MppDependency,
    newDescriptor: MppDependency,
  ) = updateDependency(module, MppModifierUpdateData(sourceSet, oldDescriptor, newDescriptor))

  suspend fun updateDependency(
    module: Module,
    data: MppModifierUpdateData
  ) = updateDependencies(module, listOf(data))

  suspend fun updateDependencies(
    module: Module,
    data: List<MppModifierUpdateData>
  ) {
    modifyKotlinModel(module) { kotlinModel ->
      data.map { (sourceSet, oldDescriptor, newDescriptor) ->
        // TODO test it when we will actually use different classes for descriptors
        if (oldDescriptor::class != newDescriptor::class) return@map

        when (oldDescriptor) {
          is MppDependency.Maven -> updateInKotlinModel(
                                      kotlinModel,
                                      oldDescriptor,
                                      newDescriptor as MppDependency.Maven,
                                      sourceSet
                                    )
          is MppDependency.Npm -> TODO()
          is MppDependency.Cocoapods -> TODO()
        }
      }
    }
  }

  private fun updateInKotlinModel(
    model: KotlinDslModel,
    oldDescriptor: MppDependency.Maven,
    newDescriptor: MppDependency.Maven,
    sourceSet: String,
  ) {
    val sourceSetModel = model.sourceSets()?.get(sourceSet)
                           .alsoIfNull { LOG.warn("Requested source set ${sourceSet} not found") }
                         ?: return
    val dependencies = sourceSetModel.dependencies()
                         .alsoIfNull { LOG.warn("Dependencies block not found in source set ${sourceSet}") }
                       ?: return

    val artifactDependencyModel = dependencies.findDependency(oldDescriptor)
                                    .alsoIfNull { LOG.warn("Could not find dependency $oldDescriptor") }
                                  ?: return

    artifactDependencyModel.updateByDescriptor(oldDescriptor, newDescriptor)
  }

  private fun KotlinDslModel.getOrCreateSourceSet(sourceSet: String, createIfMissing: Boolean): SourceSetModel? =
    sourceSets()?.get(sourceSet)
      .alsoIfNull { LOG.warn("Requested source set $sourceSet not found") }
    ?: if (createIfMissing) declareSourceSet(sourceSet) else null

  private fun SourceSetModel.getOrCreateDependenciesBlock(createIfMissing: Boolean): DependenciesModel? =
    dependencies()
      .alsoIfNull { LOG.warn("Dependencies block not found in source set $name") }
    ?: if (createIfMissing) addDependenciesBlock() else null

  private fun ArtifactDependencyModel.updateByDescriptor(oldDescriptor: MppDependency.Maven, newDescriptor: MppDependency.Maven) {
    if (oldDescriptor.groupId != newDescriptor.groupId) {
      updateVariableOrValue(group(), newDescriptor.groupId)
    }
    if (oldDescriptor.artifactId != newDescriptor.artifactId) {
      updateVariableOrValue(name(), newDescriptor.artifactId)
    }
    if (oldDescriptor.version != newDescriptor.version && newDescriptor.version != null) {
      updateVariableOrValue(version(), newDescriptor.version)
    }
    if (oldDescriptor.configuration != newDescriptor.configuration) {
      setConfigurationName(newDescriptor.configuration)
    }
  }

  private fun updateVariableOrValue(model: ResolvedPropertyModel, value: String) {
    if (model.dependencies.size == 1) {
      val dependencyPropertyModel = model.dependencies[0]
      if (dependencyPropertyModel.propertyType == PropertyType.VARIABLE ||
          dependencyPropertyModel.propertyType == PropertyType.REGULAR ||
          dependencyPropertyModel.propertyType == PropertyType.PROPERTIES_FILE
      ) {
        if (dependencyPropertyModel.valueAsString().equals(model.valueAsString())) { // not partial injection, like "${version}-SNAPSHOT"
          dependencyPropertyModel.setValue(value)
          return
        }
      }
    }
    model.setValue(value)
  }

  private fun DependenciesModel.findDependency(mavenDependency: MppDependency.Maven): ArtifactDependencyModel? {
    for (artifactModel in artifacts()) {
      if (artifactModel.group().valueAsString() == mavenDependency.groupId
          && artifactModel.name().valueAsString() == mavenDependency.artifactId
          && artifactModel.version().valueAsString() == mavenDependency.version
          && artifactModel.configurationName() == mavenDependency.configuration
      ) {
        return artifactModel
      }
    }
    return null
  }

  private suspend fun <T> readFromKotlinModel(
    module: Module,
    action: (KotlinDslModel) -> T,
  ): T? = readAction {
    module.buildModel()?.getModel<KotlinDslModel>()?.let { action(it) }
  }

  private suspend fun modifyKotlinModel(
    module: Module,
    action: (KotlinDslModel) -> Unit,
  ) = readAction {
    module.buildModel()
      ?.also { action(it.getModel()) }
  }
    ?.let { applyChanges(it) }

  private fun applyChanges(model: GradleBuildModel) {
    WriteCommandAction.writeCommandAction(model.project, model.psiFile).run<Throwable> {
      model.applyChanges()
    }
  }

  private fun Module.buildModel() = ProjectBuildModel.get(project).getModuleBuildModel(this)
    .alsoIfNull { LOG.warn("Could not create gradle model for module $this") }

  private fun MppDependency.Maven.artifactDependencySpec(): ArtifactDependencySpec =
    ArtifactDependencySpec.create(
      artifactId,
      groupId,
      version
    )

  private inline fun <reified T : GradleDslModel> GradleBuildModel.getModel() = getModel(T::class.java)
}

data class MppModifierAddData(
  val sourceSet: String,
  val descriptor: MppDependency,
)

typealias MppModifierRemoveData = MppModifierAddData

data class MppModifierUpdateData(
  val sourceSet: String,
  val oldDescriptor: MppDependency,
  val newDescriptor: MppDependency
)