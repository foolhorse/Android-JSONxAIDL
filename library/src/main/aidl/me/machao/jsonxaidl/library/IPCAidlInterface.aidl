// IPCAidlInterface.aidl
package me.machao.jsonxaidl.library;

// Declare any non-default types here with import statements

interface IPCAidlInterface {

      String call(String request);

      void gc(in List<String> objIdList);

      void setCallbackIInterface(int pid, in IBinder iBinder);
}
