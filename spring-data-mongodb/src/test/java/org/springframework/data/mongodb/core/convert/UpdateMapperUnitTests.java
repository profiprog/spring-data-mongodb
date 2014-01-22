/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.DBObjectTestUtils;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.DBObject;

/**
 * Unit tests for {@link UpdateMapper}.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdateMapperUnitTests {

	@Mock MongoDbFactory factory;
	MappingMongoConverter converter;
	MongoMappingContext context;

	@Before
	public void setUp() {

		this.context = new MongoMappingContext();
		this.converter = new MappingMongoConverter(new DefaultDbRefResolver(factory), context);
	}

	/**
	 * @see DATAMONGO-721
	 */
	@Test
	public void updateMapperRetainsTypeInformationForCollectionField() {

		Update update = new Update().push("list", new ConcreteChildClass("2", "BAR"));
		UpdateMapper mapper = new UpdateMapper(converter);

		DBObject mappedObject = mapper.getMappedObject(update.getUpdateObject(),
				context.getPersistentEntity(ParentClass.class));

		DBObject push = DBObjectTestUtils.getAsDBObject(mappedObject, "$push");
		DBObject list = DBObjectTestUtils.getAsDBObject(push, "list");

		assertThat(list.get("_class"), is((Object) ConcreteChildClass.class.getName()));
	}

	/**
	 * @see DATAMONGO-807
	 */
	@Test
	public void updateMapperShouldRetainTypeInformationForNestedEntities() {

		Update update = Update.update("model", ModelImpl.value(1));
		UpdateMapper mapper = new UpdateMapper(converter);

		DBObject mappedObject = mapper.getMappedObject(update.getUpdateObject(),
				context.getPersistentEntity(ModelWrapper.class));

		DBObject set = DBObjectTestUtils.getAsDBObject(mappedObject, "$set");
		DBObject modelDbObject = (DBObject) set.get("model");
		assertThat(modelDbObject.get("_class"), not(nullValue()));
	}

	/**
	 * @see DATAMONGO-807
	 */
	@Test
	public void updateMapperShouldNotPersistTypeInformationForKnownSimpleTypes() {

		Update update = Update.update("model.value", 1);
		UpdateMapper mapper = new UpdateMapper(converter);

		DBObject mappedObject = mapper.getMappedObject(update.getUpdateObject(),
				context.getPersistentEntity(ModelWrapper.class));

		DBObject set = DBObjectTestUtils.getAsDBObject(mappedObject, "$set");
		assertThat(set.get("_class"), nullValue());
	}

	/**
	 * @see DATAMONGO-807
	 */
	@Test
	public void updateMapperShouldNotPersistTypeInformationForNullValues() {

		Update update = Update.update("model", null);
		UpdateMapper mapper = new UpdateMapper(converter);

		DBObject mappedObject = mapper.getMappedObject(update.getUpdateObject(),
				context.getPersistentEntity(ModelWrapper.class));

		DBObject set = DBObjectTestUtils.getAsDBObject(mappedObject, "$set");
		assertThat(set.get("_class"), nullValue());
	}

	static interface Model {

	}

	static class ModelImpl implements Model {
		public int value;

		static ModelImpl value(int value) {
			ModelImpl impl = new ModelImpl();
			impl.value = value;
			return impl;
		}
	}

	public class ModelWrapper {
		Model model;
	}

	static class ParentClass {

		String id;
		List<? extends AbstractChildClass> list;

		public ParentClass(String id, List<? extends AbstractChildClass> list) {
			this.id = id;
			this.list = list;
		}

	}

	static abstract class AbstractChildClass {

		String id;
		String value;

		public AbstractChildClass(String id, String value) {
			this.id = id;
			this.value = value;
		}
	}

	static class ConcreteChildClass extends AbstractChildClass {

		public ConcreteChildClass(String id, String value) {
			super(id, value);
		}
	}
}
