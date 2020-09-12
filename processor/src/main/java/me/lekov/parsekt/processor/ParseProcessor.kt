package me.lekov.parsekt.processor

import com.squareup.kotlinpoet.*
import me.lekov.parsekt.annotations.ParseClassName
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(ParseProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class ParseProcessor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(ParseClassName::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun process(
        set: MutableSet<out TypeElement>?,
        roundEnvironment: RoundEnvironment?
    ): Boolean {
        println("Processor is being init!!")

        val kaptKotlinGeneratedDir =
            processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
                ?: return false

        val parseClasses = roundEnvironment?.getElementsAnnotatedWith(ParseClassName::class.java)

        if (parseClasses?.isEmpty() != false) {
            return true
        }

        val parseClass = TypeSpec.enumBuilder("ParseClasses")
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("parseName", String::class, KModifier.PUBLIC)
                    .build()
            )
            .addProperty(PropertySpec.builder("parseName", String::class).initializer("parseName").build())

        parseClass.addEnumConstant(
            "ParseUser", TypeSpec.anonymousClassBuilder()
                .addSuperclassConstructorParameter("%S", "_User")
                .build()
        )

        parseClasses
            .forEach {
                val className = it.simpleName.toString()
                val pack = processingEnv.elementUtils.getPackageOf(it).toString()

                parseClass.addEnumConstant(
                    className, TypeSpec.anonymousClassBuilder()
                        .addSuperclassConstructorParameter(
                            "%S", it.getAnnotation(ParseClassName::class.java).value
                        )
                        .build()
                )
            }

        FileSpec.builder("me.lekov.parsekt.types", "ParseClasses").addType(parseClass.build())
            .build().writeTo(File(kaptKotlinGeneratedDir))

        return true
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

}