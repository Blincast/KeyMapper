package io.github.sds100.keymapper

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import timber.log.Timber

const val DB_NAME = "mydb.db"
const val DB_VERSION = 2

const val AUTHORITY = "io.github.sds100.keymapper"
const val TABLE_NAME = "usage"
const val COLUMN_ID = "_id"
val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$TABLE_NAME")

class MyContentProvider : ContentProvider() {
    private lateinit var dbHelper: DBHelper

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI("io.github.sds100.keymapper", TABLE_NAME, 1)
        addURI("io.github.sds100.keymapper", "$TABLE_NAME/#", 2)
    }

    override fun onCreate(): Boolean {
        dbHelper = DBHelper(context!!)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor
        when (uriMatcher.match(uri)) {
            1 -> {
                cursor = db.query(
                    TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
                )
            }
            2 -> {
                val id = uri.lastPathSegment
                cursor = db.query(
                    TABLE_NAME,
                    projection,
                    "$COLUMN_ID=?",
                    arrayOf(id),
                    null,
                    null,
                    sortOrder
                )
            }
            else -> return null
        }
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            1 -> "vnd.android.cursor.dir/$AUTHORITY.$TABLE_NAME"
            2 -> "vnd.android.cursor.item/$AUTHORITY.$TABLE_NAME"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = dbHelper.writableDatabase
        val id = when (uriMatcher.match(uri)) {
            1 -> {
                db.insert(TABLE_NAME, null, values)
            }
            else -> return null
        }
        context!!.contentResolver.notifyChange(uri, null)
        return Uri.withAppendedPath(CONTENT_URI, id.toString())
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = dbHelper.writableDatabase
        val count: Int
        when (uriMatcher.match(uri)) {
            1 -> {
                count = db.delete(TABLE_NAME, selection, selectionArgs)
            }
            2 -> {
                val id = uri.lastPathSegment
                count = db.delete(
                    TABLE_NAME,
                    "$COLUMN_ID=?",
                    arrayOf(id)
                )
            }
            else -> return 0
        }
        context!!.contentResolver.notifyChange(uri, null)
        return count
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val db = dbHelper.writableDatabase
        val count = when (uriMatcher.match(uri)) {
            1 -> {
                db.update(TABLE_NAME, values, selection, selectionArgs)
            }
            2 -> {
                val id = uri.lastPathSegment
                db.update(
                    TABLE_NAME,
                    values,
                    "$COLUMN_ID=?",
                    arrayOf(id)
                )
            }
            else -> 0
        }
        context!!.contentResolver.notifyChange(uri, null)
        return count
    }
}

class DBHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        Timber.tag("DATABASE").i("Creating database")

        val SQL_CREATE_DATA_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                package_name TEXT NOT NULL,
                start_time INT NOT NULL,
                end_time INT,
                launch BOOLEAN NOT NULL
            )
        """.trimIndent()
        db.execSQL(SQL_CREATE_DATA_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }
}