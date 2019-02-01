package me.machao.jsonxaidl.sample;

import me.machao.jsonxaidl.library.ImplClass;

/**
 * Date  2019/1/30
 *
 * @author charliema
 */
@ImplClass("me.machao.jsonxaidl.sample.User")
public interface IUser {
    String getName();
    void setName(String name);

    void setCallback(Call call);

}
