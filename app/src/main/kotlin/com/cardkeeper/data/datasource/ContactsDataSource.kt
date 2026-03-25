package com.cardkeeper.data.datasource

import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ContactsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun createExportIntent(
        name: String,
        company: String,
        jobTitle: String,
        phone: String,
        email: String
    ): Intent {
        return Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, name)
            putExtra(ContactsContract.Intents.Insert.COMPANY, company)
            putExtra(ContactsContract.Intents.Insert.JOB_TITLE, jobTitle)
            if (phone.isNotBlank()) {
                putExtra(
                    ContactsContract.Intents.Insert.PHONE,
                    phone
                )
                putExtra(
                    ContactsContract.Intents.Insert.PHONE_TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK
                )
            }
            if (email.isNotBlank()) {
                putExtra(
                    ContactsContract.Intents.Insert.EMAIL,
                    email
                )
                putExtra(
                    ContactsContract.Intents.Insert.EMAIL_TYPE,
                    ContactsContract.CommonDataKinds.Email.TYPE_WORK
                )
            }
        }
    }
}
