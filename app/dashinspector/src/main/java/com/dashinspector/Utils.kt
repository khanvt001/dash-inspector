package com.dashinspector

// Extension functions for request validation
fun PrefRequest.isValidForCreate(): Boolean {
    return !name.isNullOrEmpty() && !key.isNullOrEmpty() && !type.isNullOrEmpty()
}

fun PrefRequest.isValueValidForType(): Boolean {
    // String and Set types can have null/empty values
    if (type == "String" || type == "Set" || type == "StringSet") {
        return true
    }
    return !value.isNullOrEmpty()
}