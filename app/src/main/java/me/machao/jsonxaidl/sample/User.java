package me.machao.jsonxaidl.sample;

import android.os.Handler;

/**
 * Date  2019/1/30
 *
 * @author charliema
 */
public class User implements IUser {

    private String id;
    private String name;

    private Call call;

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public User() {
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setCallback(final Call call) {
        this.call = call;


        call.invoke();


    }
}
