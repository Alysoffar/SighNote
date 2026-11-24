package com.notes.ink.persistence;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class NotesDatabase_Impl extends NotesDatabase {
  private volatile StrokeDao _strokeDao;

  private volatile PageDao _pageDao;

  private volatile NotebookDao _notebookDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `strokes` (`id` TEXT NOT NULL, `page_id` TEXT NOT NULL, `brush_id` TEXT NOT NULL, `min_x` REAL NOT NULL, `min_y` REAL NOT NULL, `max_x` REAL NOT NULL, `max_y` REAL NOT NULL, `points_blob` BLOB NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_strokes_page_id` ON `strokes` (`page_id`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `pages` (`id` TEXT NOT NULL, `notebook_id` TEXT NOT NULL, `revision_counter` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `notebooks` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '14aaa727b9040ec2dd43ac1baeb94c49')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `strokes`");
        db.execSQL("DROP TABLE IF EXISTS `pages`");
        db.execSQL("DROP TABLE IF EXISTS `notebooks`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsStrokes = new HashMap<String, TableInfo.Column>(9);
        _columnsStrokes.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStrokes.put("page_id", new TableInfo.Column("page_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStrokes.put("brush_id", new TableInfo.Column("brush_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStrokes.put("min_x", new TableInfo.Column("min_x", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStrokes.put("min_y", new TableInfo.Column("min_y", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStrokes.put("max_x", new TableInfo.Column("max_x", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStrokes.put("max_y", new TableInfo.Column("max_y", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStrokes.put("points_blob", new TableInfo.Column("points_blob", "BLOB", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStrokes.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStrokes = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStrokes = new HashSet<TableInfo.Index>(1);
        _indicesStrokes.add(new TableInfo.Index("index_strokes_page_id", false, Arrays.asList("page_id"), Arrays.asList("ASC")));
        final TableInfo _infoStrokes = new TableInfo("strokes", _columnsStrokes, _foreignKeysStrokes, _indicesStrokes);
        final TableInfo _existingStrokes = TableInfo.read(db, "strokes");
        if (!_infoStrokes.equals(_existingStrokes)) {
          return new RoomOpenHelper.ValidationResult(false, "strokes(com.notes.ink.persistence.StrokeEntity).\n"
                  + " Expected:\n" + _infoStrokes + "\n"
                  + " Found:\n" + _existingStrokes);
        }
        final HashMap<String, TableInfo.Column> _columnsPages = new HashMap<String, TableInfo.Column>(5);
        _columnsPages.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPages.put("notebook_id", new TableInfo.Column("notebook_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPages.put("revision_counter", new TableInfo.Column("revision_counter", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPages.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPages.put("updated_at", new TableInfo.Column("updated_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPages = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPages = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPages = new TableInfo("pages", _columnsPages, _foreignKeysPages, _indicesPages);
        final TableInfo _existingPages = TableInfo.read(db, "pages");
        if (!_infoPages.equals(_existingPages)) {
          return new RoomOpenHelper.ValidationResult(false, "pages(com.notes.ink.persistence.PageEntity).\n"
                  + " Expected:\n" + _infoPages + "\n"
                  + " Found:\n" + _existingPages);
        }
        final HashMap<String, TableInfo.Column> _columnsNotebooks = new HashMap<String, TableInfo.Column>(4);
        _columnsNotebooks.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotebooks.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotebooks.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotebooks.put("updated_at", new TableInfo.Column("updated_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysNotebooks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesNotebooks = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoNotebooks = new TableInfo("notebooks", _columnsNotebooks, _foreignKeysNotebooks, _indicesNotebooks);
        final TableInfo _existingNotebooks = TableInfo.read(db, "notebooks");
        if (!_infoNotebooks.equals(_existingNotebooks)) {
          return new RoomOpenHelper.ValidationResult(false, "notebooks(com.notes.ink.persistence.NotebookEntity).\n"
                  + " Expected:\n" + _infoNotebooks + "\n"
                  + " Found:\n" + _existingNotebooks);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "14aaa727b9040ec2dd43ac1baeb94c49", "f331481cc3eda547de1e72c86fb34915");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "strokes","pages","notebooks");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `strokes`");
      _db.execSQL("DELETE FROM `pages`");
      _db.execSQL("DELETE FROM `notebooks`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(StrokeDao.class, StrokeDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PageDao.class, PageDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(NotebookDao.class, NotebookDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public StrokeDao strokeDao() {
    if (_strokeDao != null) {
      return _strokeDao;
    } else {
      synchronized(this) {
        if(_strokeDao == null) {
          _strokeDao = new StrokeDao_Impl(this);
        }
        return _strokeDao;
      }
    }
  }

  @Override
  public PageDao pageDao() {
    if (_pageDao != null) {
      return _pageDao;
    } else {
      synchronized(this) {
        if(_pageDao == null) {
          _pageDao = new PageDao_Impl(this);
        }
        return _pageDao;
      }
    }
  }

  @Override
  public NotebookDao notebookDao() {
    if (_notebookDao != null) {
      return _notebookDao;
    } else {
      synchronized(this) {
        if(_notebookDao == null) {
          _notebookDao = new NotebookDao_Impl(this);
        }
        return _notebookDao;
      }
    }
  }
}
