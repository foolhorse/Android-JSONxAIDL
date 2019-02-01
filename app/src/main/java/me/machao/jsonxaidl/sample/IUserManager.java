package me.machao.jsonxaidl.sample;


import me.machao.jsonxaidl.library.ImplClass;

/**
 * Date  2019/1/25
 *
 * @author charliema
 */
@ImplClass("me.machao.jsonxaidl.sample.UserManager")
public interface IUserManager {

    User getUser();
    void setUser(User user);
}
