/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSAttributeEnumerable;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * JDBC abstract table column
 */
public abstract class JDBCTableColumn<TABLE_TYPE extends DBSEntity> extends JDBCAttribute implements DBSTableColumn, DBSAttributeEnumerable, DBPSaveableObject {

    private final TABLE_TYPE table;
    private boolean persisted;
    private String defaultValue;

    protected JDBCTableColumn(TABLE_TYPE table, boolean persisted)
    {
        this.table = table;
        this.persisted = persisted;
    }

    protected JDBCTableColumn(
            TABLE_TYPE table,
            boolean persisted,
            String name,
            String typeName,
            int valueType,
            int ordinalPosition,
            long maxLength,
            int scale,
            int precision,
            boolean required,
            boolean autoGenerated,
            String defaultValue)
    {
        super(name, typeName, valueType, ordinalPosition, maxLength, scale, precision, required, autoGenerated);
        this.defaultValue = defaultValue;
        this.table = table;
        this.persisted = persisted;
    }

    public TABLE_TYPE getTable()
    {
        return table;
    }

    @NotNull
    @Override
    public TABLE_TYPE getParentObject()
    {
        return getTable();
    }

    @NotNull
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 10)
    @Override
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, editable = true, order = 20, listProvider = ColumnTypeNameListProvider.class)
    @Override
    public String getTypeName()
    {
        return super.getTypeName();
    }

    @Override
    public void setTypeName(String typeName) {
        super.setTypeName(typeName);
        final DBPDataSource dataSource = getDataSource();
        if (dataSource instanceof DBPDataTypeProvider) {
            DBSDataType dataType = ((DBPDataTypeProvider) dataSource).getLocalDataType(typeName);
            if (dataType != null) {
                this.valueType = dataType.getTypeID();
            }
        }
    }

    @Property(viewable = true, editable = true, order = 40)
    @Override
    public long getMaxLength()
    {
        return super.getMaxLength();
    }

    @Property(viewable = true, editable = true, order = 50)
    @Override
    public boolean isRequired()
    {
        return super.isRequired();
    }

    @Property(viewable = true, editable = true, order = 70)
    @Override
    public String getDefaultValue()
    {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }

    @Override
    public Collection<DBDLabelValuePair> getValueEnumeration(DBCSession session, Object valuePattern, int maxResults) throws DBException {
        DBDValueHandler valueHandler = DBUtils.findValueHandler(session, this);
        StringBuilder query = new StringBuilder();
        query.append("SELECT UNIQUE ").append(DBUtils.getQuotedIdentifier(this));
        String descColumns = DBVUtils.getDictionaryDescriptionColumns(session.getProgressMonitor(), this);
        if (descColumns != null) {
            query.append(", ").append(descColumns);
        }
        query.append(" FROM ").append(DBUtils.getObjectFullName(getTable()));

        try (DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, false)) {
            dbStat.setLimit(0, maxResults);
            if (dbStat.executeStatement()) {
                try (DBCResultSet dbResult = dbStat.openResultSet()) {
                    return DBVUtils.readDictionaryRows(session, this, valueHandler, dbResult);
                }
            } else {
                return Collections.emptyList();
            }
        }
    }

    public static class ColumnTypeNameListProvider implements IPropertyValueListProvider<JDBCTableColumn> {

        @Override
        public boolean allowCustomValue()
        {
            return true;
        }

        @Override
        public Object[] getPossibleValues(JDBCTableColumn column)
        {
            Set<String> typeNames = new TreeSet<>();
            if (column.getDataSource() instanceof DBPDataTypeProvider) {
                for (DBSDataType type : ((DBPDataTypeProvider) column.getDataSource()).getLocalDataTypes()) {
                    if (type.getDataKind() != DBPDataKind.UNKNOWN && !CommonUtils.isEmpty(type.getName()) && Character.isLetter(type.getName().charAt(0))) {
                        typeNames.add(type.getName());
                    }
                }
            }
            return typeNames.toArray(new String[typeNames.size()]);
        }
    }

}
