package com.notes.ink.persistence;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class StrokeDao_Impl implements StrokeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<StrokeEntity> __insertionAdapterOfStrokeEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteStrokeById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllStrokesForPage;

  public StrokeDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfStrokeEntity = new EntityInsertionAdapter<StrokeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `strokes` (`id`,`page_id`,`brush_id`,`min_x`,`min_y`,`max_x`,`max_y`,`points_blob`,`created_at`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StrokeEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getPageId());
        statement.bindString(3, entity.getBrushId());
        statement.bindDouble(4, entity.getMinX());
        statement.bindDouble(5, entity.getMinY());
        statement.bindDouble(6, entity.getMaxX());
        statement.bindDouble(7, entity.getMaxY());
        statement.bindBlob(8, entity.getPointsBlob());
        statement.bindLong(9, entity.getCreatedAt());
      }
    };
    this.__preparedStmtOfDeleteStrokeById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM strokes WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllStrokesForPage = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM strokes WHERE page_id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertStroke(final StrokeEntity stroke,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStrokeEntity.insert(stroke);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertStrokes(final List<StrokeEntity> strokes,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStrokeEntity.insert(strokes);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteStrokeById(final String strokeId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteStrokeById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, strokeId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteStrokeById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllStrokesForPage(final String pageId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllStrokesForPage.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, pageId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllStrokesForPage.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<StrokeEntity>> getStrokesForPage(final String pageId) {
    final String _sql = "SELECT * FROM strokes WHERE page_id = ? ORDER BY created_at ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, pageId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"strokes"}, new Callable<List<StrokeEntity>>() {
      @Override
      @NonNull
      public List<StrokeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPageId = CursorUtil.getColumnIndexOrThrow(_cursor, "page_id");
          final int _cursorIndexOfBrushId = CursorUtil.getColumnIndexOrThrow(_cursor, "brush_id");
          final int _cursorIndexOfMinX = CursorUtil.getColumnIndexOrThrow(_cursor, "min_x");
          final int _cursorIndexOfMinY = CursorUtil.getColumnIndexOrThrow(_cursor, "min_y");
          final int _cursorIndexOfMaxX = CursorUtil.getColumnIndexOrThrow(_cursor, "max_x");
          final int _cursorIndexOfMaxY = CursorUtil.getColumnIndexOrThrow(_cursor, "max_y");
          final int _cursorIndexOfPointsBlob = CursorUtil.getColumnIndexOrThrow(_cursor, "points_blob");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<StrokeEntity> _result = new ArrayList<StrokeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StrokeEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPageId;
            _tmpPageId = _cursor.getString(_cursorIndexOfPageId);
            final String _tmpBrushId;
            _tmpBrushId = _cursor.getString(_cursorIndexOfBrushId);
            final float _tmpMinX;
            _tmpMinX = _cursor.getFloat(_cursorIndexOfMinX);
            final float _tmpMinY;
            _tmpMinY = _cursor.getFloat(_cursorIndexOfMinY);
            final float _tmpMaxX;
            _tmpMaxX = _cursor.getFloat(_cursorIndexOfMaxX);
            final float _tmpMaxY;
            _tmpMaxY = _cursor.getFloat(_cursorIndexOfMaxY);
            final byte[] _tmpPointsBlob;
            _tmpPointsBlob = _cursor.getBlob(_cursorIndexOfPointsBlob);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new StrokeEntity(_tmpId,_tmpPageId,_tmpBrushId,_tmpMinX,_tmpMinY,_tmpMaxX,_tmpMaxY,_tmpPointsBlob,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getStrokesListForPage(final String pageId,
      final Continuation<? super List<StrokeEntity>> $completion) {
    final String _sql = "SELECT * FROM strokes WHERE page_id = ? ORDER BY created_at ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, pageId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<StrokeEntity>>() {
      @Override
      @NonNull
      public List<StrokeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPageId = CursorUtil.getColumnIndexOrThrow(_cursor, "page_id");
          final int _cursorIndexOfBrushId = CursorUtil.getColumnIndexOrThrow(_cursor, "brush_id");
          final int _cursorIndexOfMinX = CursorUtil.getColumnIndexOrThrow(_cursor, "min_x");
          final int _cursorIndexOfMinY = CursorUtil.getColumnIndexOrThrow(_cursor, "min_y");
          final int _cursorIndexOfMaxX = CursorUtil.getColumnIndexOrThrow(_cursor, "max_x");
          final int _cursorIndexOfMaxY = CursorUtil.getColumnIndexOrThrow(_cursor, "max_y");
          final int _cursorIndexOfPointsBlob = CursorUtil.getColumnIndexOrThrow(_cursor, "points_blob");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<StrokeEntity> _result = new ArrayList<StrokeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StrokeEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPageId;
            _tmpPageId = _cursor.getString(_cursorIndexOfPageId);
            final String _tmpBrushId;
            _tmpBrushId = _cursor.getString(_cursorIndexOfBrushId);
            final float _tmpMinX;
            _tmpMinX = _cursor.getFloat(_cursorIndexOfMinX);
            final float _tmpMinY;
            _tmpMinY = _cursor.getFloat(_cursorIndexOfMinY);
            final float _tmpMaxX;
            _tmpMaxX = _cursor.getFloat(_cursorIndexOfMaxX);
            final float _tmpMaxY;
            _tmpMaxY = _cursor.getFloat(_cursorIndexOfMaxY);
            final byte[] _tmpPointsBlob;
            _tmpPointsBlob = _cursor.getBlob(_cursorIndexOfPointsBlob);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new StrokeEntity(_tmpId,_tmpPageId,_tmpBrushId,_tmpMinX,_tmpMinY,_tmpMaxX,_tmpMaxY,_tmpPointsBlob,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getStrokeCount(final String pageId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM strokes WHERE page_id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, pageId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
