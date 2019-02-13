package me.machao.jsonxaidl.library.model

/**
 * Date  2019/1/25
 *
 * @author charliema
 */

data class Request(

    var type: Short,

    var pid: Int,

    var objId: String?,

    var className: String?,
    var methodName: String?,
    var requestParameters: Array<RequestParameter>?
){
    companion object {
        const val NEW_INSTANCE: Short = 1
        const val GET_SINGLETON_INSTANCE: Short = 2
        const val INVOKE_METHOD: Short = 3
    }
}

data class RequestParameter(
    var className: String,  // TODO 改成 Class<*> ?
    var value: String?,
    var callbackObjId:String?
)

data class Response(
    val data: Any)
