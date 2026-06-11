package com.ipdial.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import com.ipdial.data.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsRepository(private val context: Context) {

    suspend fun getContacts(query: String? = null): List<Contact> = withContext(Dispatchers.IO) {
        val contactsMap = mutableMapOf<String, Contact>()
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return@withContext emptyList()
        }
        try {
            val contentResolver: ContentResolver = context.contentResolver

            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ContactsContract.CommonDataKinds.Phone.STARRED
            )

            val selection = if (query.isNullOrBlank()) {
                null
            } else {
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            }
            val selectionArgs = if (query.isNullOrBlank()) null else arrayOf("%$query%")

            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val starredIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

                while (it.moveToNext()) {
                    val id = it.getString(idIndex)
                    val name = it.getString(nameIndex) ?: "Unknown"
                    val number = it.getString(numberIndex) ?: ""
                    val photoUriStr = it.getString(photoIndex)
                    val isFavorite = it.getInt(starredIndex) == 1

                    val existingContact = contactsMap[id]
                    if (existingContact != null) {
                        if (number.isNotBlank() && !existingContact.numbers.contains(number)) {
                            contactsMap[id] = existingContact.copy(numbers = existingContact.numbers + number)
                        }
                    } else {
                        val photoUri = photoUriStr?.let { Uri.parse(it) }
                        contactsMap[id] = Contact(id, name, listOf(number), photoUri, isFavorite)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Failed to fetch contacts: ${e.message}")
        }
        contactsMap.values.toList()
    }

    suspend fun toggleFavorite(contactId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        val values = android.content.ContentValues().apply {
            put(ContactsContract.Contacts.STARRED, if (isFavorite) 1 else 0)
        }
        context.contentResolver.update(
            ContactsContract.Contacts.CONTENT_URI,
            values,
            ContactsContract.Contacts._ID + " = ?",
            arrayOf(contactId)
        )
    }
}
