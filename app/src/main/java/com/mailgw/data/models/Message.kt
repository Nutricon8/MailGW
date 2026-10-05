// data/models/Message.kt
package com.mailgw.data.models

import com.google.gson.annotations.SerializedName

data class MessagesResponse(
    @SerializedName("hydra:member")
    val members: List<MessageSummary>,
    @SerializedName("hydra:totalItems")
    val totalItems: Int
)

data class MessageSummary(
    @SerializedName("@id")
    val id: String,
    val msgid: String,
    val from: Sender,
    val subject: String,
    val intro: String,
    val seen: Boolean,
    val hasAttachments: Boolean,
    val size: Int,
    val createdAt: String
)

data class MessageDetail(
    @SerializedName("@id")
    val id: String,
    val msgid: String,
    val from: Sender,
    val to: List<Recipient>,
    val subject: String,
    val seen: Boolean,
    val text: String?,
    val html: List<String>?,
    val hasAttachments: Boolean,
    val attachments: List<Attachment>?,
    val size: Int,
    val createdAt: String
)

data class Sender(
    val name: String?,
    val address: String
)

data class Recipient(
    val name: String?,
    val address: String
)

data class Attachment(
    val id: String,
    val filename: String,
    val contentType: String,
    val size: Int,
    val downloadUrl: String
)