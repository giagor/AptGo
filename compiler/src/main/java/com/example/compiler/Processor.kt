package com.example.compiler

import com.example.annotation.ExtractField
import com.squareup.kotlinpoet.*
import java.io.File
import java.lang.Exception
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.tools.Diagnostic

class Processor : AbstractProcessor() {
    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

        // 日志文件的路径
        const val LOG_FILE_PATH = "D:\\AndroidStudioProjects\\demo\\log.txt"
    }

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        createLogFile()
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(ExtractField::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(
        annotations: MutableSet<out TypeElement>?,
        roundEnv: RoundEnvironment
    ): Boolean {
        logInfo("Processor.process 方法被调用")
        val set = roundEnv.getElementsAnnotatedWith(ExtractField::class.java)
        if (set == null || set.isEmpty()) {
            return false
        }

        set.forEach { element ->
            if (element.kind != ElementKind.CLASS) {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Only classes can be annotated"
                )
                return@forEach
            }
            processAnnotation(element)
        }
        return true
    }

    private fun processAnnotation(element: Element) {
        // 获取元素类名、包名
        val className = element.simpleName.toString()
        val pack = processingEnv.elementUtils.getPackageOf(element).toString()
        // 生成类的类名
        val fileName = "ExtractField$className"
        // 表示一个 kotlin 文件，指定包名和类名
        val fileBuilder = FileSpec.builder(pack, fileName)
        // 表示要生成的类
        val classBuilder = TypeSpec.classBuilder(fileName)

        logInfo("className：$className", "pack：$pack", "fileName：$fileName")
        // 获取 Element 的子节点，只对字段进行处理
        for (childElement in element.enclosedElements) {
            if (childElement.kind == ElementKind.FIELD) {
                // 向类里添加字段
                addProperty(classBuilder, childElement)
                logInfo("FieldType：${childElement.asType().asTypeName().asNullable()}")
                // 向类里添加字段的 get 方法
                addGetFunc(classBuilder, childElement)
                // 向类里添加字段的 set 方法
                addSetFunc(classBuilder, childElement)
            }
        }
        // 向 fileBuilder 表示的 kotlin 文件中写入 classBuilder 类
        val file = fileBuilder.addType(classBuilder.build()).build()
        // 获取生成的文件所在的目录
        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        logInfo("kaptKotlinGeneratedDir：$kaptKotlinGeneratedDir")
        // 将 file 表示的文件写入目录 
        file.writeTo(File(kaptKotlinGeneratedDir))
    }

    // 往 classBuilder 代表的类中添加 element 字段，其中类型为可空的，初始值为 null  
    private fun addProperty(classBuilder: TypeSpec.Builder, element: Element) {
        classBuilder.addProperty(
            PropertySpec.varBuilder(
                element.simpleName.toString(),
                element.asType().asTypeName().asNullable(),
                KModifier.PRIVATE
            )
                .initializer("null")
                .build()
        )
    }

    // 往 classBuilder 代表的类中添加 element 的 getter 方法
    private fun addGetFunc(classBuilder: TypeSpec.Builder, element: Element) {
        classBuilder.addFunction(
            FunSpec.builder("getThe${element.simpleName}")
                .returns(element.asType().asTypeName().asNullable())
                .addStatement("return ${element.simpleName}")
                .build()
        )
    }

    // 往 classBuilder 代表的类中添加 element 的 setter 方法
    private fun addSetFunc(classBuilder: TypeSpec.Builder, element: Element) {
        classBuilder.addFunction(
            FunSpec.builder("setThe${element.simpleName}")
                .addParameter(
                    ParameterSpec.builder(
                        "${element.simpleName}",
                        element.asType().asTypeName().asNullable()
                    ).build()
                )
                .addStatement("this.${element.simpleName} = ${element.simpleName}")
                .build()
        )
    }

    // 创建日志文件
    private fun createLogFile() {
        kotlin.runCatching {
            val logFile = File(LOG_FILE_PATH)
            if (logFile.exists()) {
                logFile.delete()
            }
            logFile.createNewFile()
        }
    }

    // 把信息写到日志文件中，每写入一条信息，就换行一次
    private fun logInfo(vararg info: String) {
        kotlin.runCatching {
            val logFile = File(LOG_FILE_PATH)
            if (logFile.exists()) {
                info.forEach {
                    logFile.appendText(it)
                    logFile.appendText("\n")
                }
            }
        }
    }
}