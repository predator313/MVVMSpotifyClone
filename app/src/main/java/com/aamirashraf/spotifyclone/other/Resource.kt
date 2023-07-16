package com.aamirashraf.spotifyclone.other

sealed class Resource<T>(
    val data:T?=null,
    val message:String?=null
){
    //Resource class is recommended by google
    //and this will be a sealed class means only certain numbers of class are required to inherit from
    //the resource class
    class Success<T>(data: T):Resource<T>(data)
    class Error<T>(message: String,data: T?=null):Resource<T>(data,message)
    class Loading<T>:Resource<T>()
}