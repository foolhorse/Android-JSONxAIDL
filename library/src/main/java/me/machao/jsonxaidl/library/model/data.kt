package me.machao.jsonxaidl.library.model

/**
 * Date  2019/1/25
 *
 * @author charliema
 */

data class Request(
    var pid:Int?,
    var objId:String?,

    var type: Short,
    var className: String?,
    var methodName: String?,
    var requestParameters: Array<RequestParameter>?
)

data class RequestParameter(
    var className: String,  // TODO 改成 Class<*> ?
    var value: String?,
    var callbackObjId:String?
)

data class Response(
    val data: Any)
