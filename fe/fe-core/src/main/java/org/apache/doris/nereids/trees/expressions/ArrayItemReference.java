// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.nereids.trees.expressions;

import org.apache.doris.common.Pair;
import org.apache.doris.nereids.trees.expressions.typecoercion.ExpectsInputTypes;
import org.apache.doris.nereids.trees.expressions.visitor.ExpressionVisitor;
import org.apache.doris.nereids.types.ArrayType;
import org.apache.doris.nereids.types.DataType;
import org.apache.doris.nereids.types.coercion.AnyDataType;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Objects;

/**
 * it is item from array, which used in lambda function
 */
public class ArrayItemReference extends NamedExpression implements ExpectsInputTypes {

    protected final ExprId exprId;
    protected final String name;

    /** ArrayItemReference */
    public ArrayItemReference(String name, Expression arrayExpression) {
        this(StatementScopeIdGenerator.newExprId(), name, arrayExpression);
    }

    public ArrayItemReference(ExprId exprId, String name, Expression arrayExpression) {
        super(ImmutableList.of(arrayExpression));
        Preconditions.checkArgument(arrayExpression.getDataType() instanceof ArrayType,
                String.format("ArrayItemReference' child %s must return array", child(0)));
        this.exprId = exprId;
        this.name = name;
    }

    public Expression getArrayExpression() {
        return children.get(0);
    }

    @Override
    public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitArrayItemReference(this, context);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ExprId getExprId() {
        return exprId;
    }

    @Override
    public List<String> getQualifier() {
        return ImmutableList.of(name);
    }

    @Override
    public boolean nullable() {
        return ((ArrayType) (this.children.get(0).getDataType())).containsNull();
    }

    @Override
    public ArrayItemReference withChildren(List<Expression> expressions) {
        return new ArrayItemReference(exprId, name, expressions.get(0));
    }

    @Override
    public DataType getDataType() {
        return ((ArrayType) (this.children.get(0).getDataType())).getItemType();
    }

    @Override
    public String computeToSql() {
        return child(0).toSql();
    }

    @Override
    public Slot toSlot() {
        return new ArrayItemSlot(exprId, name, getDataType(), nullable());
    }

    @Override
    public String toString() {
        String str = getName() + "#" + getExprId();
        str += " of " + child(0).toString();
        return str;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArrayItemReference that = (ArrayItemReference) o;
        return exprId.equals(that.exprId);
    }

    @Override
    public int computeHashCode() {
        return Objects.hash(exprId);
    }

    @Override
    public List<DataType> expectedInputTypes() {
        return ImmutableList.of(ArrayType.of(AnyDataType.INSTANCE_WITHOUT_INDEX));
    }

    /**
     * it is slot representation of ArrayItemReference
     */
    public static class ArrayItemSlot extends SlotReference implements SlotNotFromChildren {
        /**
         * Constructor for SlotReference.
         *
         * @param exprId UUID for this slot reference
         * @param name slot reference name
         * @param dataType slot reference logical data type
         * @param nullable true if nullable
         */
        public ArrayItemSlot(ExprId exprId, String name, DataType dataType, boolean nullable) {
            super(exprId, name, dataType, nullable, ImmutableList.of(),
                    null, null, null, null, ImmutableList.of());
        }

        @Override
        public ArrayItemSlot withExprId(ExprId exprId) {
            return new ArrayItemSlot(exprId, name.get(), dataType, nullable);
        }

        @Override
        public ArrayItemSlot withName(String name) {
            return new ArrayItemSlot(exprId, name, dataType, nullable);
        }

        @Override
        public SlotReference withNullable(boolean nullable) {
            return new ArrayItemSlot(exprId, name.get(), dataType, this.nullable);
        }

        @Override
        public Slot withNullableAndDataType(boolean nullable, DataType dataType) {
            return new ArrayItemSlot(exprId, name.get(), dataType, nullable);
        }

        @Override
        public Slot withIndexInSql(Pair<Integer, Integer> index) {
            return this;
        }

        @Override
        public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) {
            return visitor.visitArrayItemSlot(this, context);
        }
    }
}
