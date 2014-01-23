/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mongodb.core.convert;

import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;

import com.mongodb.DBObject;

/**
 * A subclass of {@link QueryMapper} that retains type information on the mongo types.
 * 
 * @author Thomas Darimont
 */
public class UpdateMapper extends QueryMapper {

	private final MongoConverter converter;

	/**
	 * Creates a new {@link UpdateMapper} using the given {@link MongoConverter}.
	 * 
	 * @param converter must not be {@literal null}.
	 */
	public UpdateMapper(MongoConverter converter) {

		super(converter);
		this.converter = converter;
	}

	/**
	 * Converts the given source object to a mongo type retaining the original type information of the source type on the
	 * mongo type.
	 * 
	 * @see org.springframework.data.mongodb.core.convert.QueryMapper#delegateConvertToMongoType(java.lang.Object,
	 *      org.springframework.data.mongodb.core.mapping.MongoPersistentEntity)
	 */
	@Override
	protected Object delegateConvertToMongoType(Object source, MongoPersistentEntity<?> entity) {
		return entity == null ? super.delegateConvertToMongoType(source, null) : converter.convertToMongoType(source,
				entity.getTypeInformation());
	}

	/**
	 * retain class type information for eg. nested types during an update, otherwise conversion will be corrupted when
	 * reading values from store
	 * 
	 * @param rawValue
	 * @param newKey
	 * @param mappedPropertyFieldValue
	 */
	private void writeClassTypeInformationForUpdateIfRequired(Object rawValue, String newKey,
			Object mappedPropertyFieldValue) {

		if (!converter.getTypeMapper().isTypeKey(newKey) && mappedPropertyFieldValue instanceof DBObject) {
			if (!converter.getConversionService().canConvert(rawValue.getClass(), DBObject.class)) {
				converter.getTypeMapper().writeType(rawValue.getClass(), (DBObject) mappedPropertyFieldValue);
			}
		}
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.mongodb.core.convert.QueryMapper#getMappedValue(org.springframework.data.mongodb.core.convert.QueryMapper.Field,
	 *      java.lang.Object)
	 */
	@Override
	protected Object getMappedValue(Field documentField, Object value) {

		Object mappedPropertyFieldValue = super.getMappedValue(documentField, value);

		writeClassTypeInformationForUpdateIfRequired(value, documentField.getMappedKey(), mappedPropertyFieldValue);

		return mappedPropertyFieldValue;
	}
}
