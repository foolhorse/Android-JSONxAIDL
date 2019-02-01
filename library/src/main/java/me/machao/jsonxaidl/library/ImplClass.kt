package me.machao.jsonxaidl.library

import kotlin.annotation.Retention

/**
 * Date  2019/1/25
 *
 * @author charliema
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class ImplClass(val value: String)
