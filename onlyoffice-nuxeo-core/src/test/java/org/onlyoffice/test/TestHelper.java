/**
 *
 * (c) Copyright Ascensio System SIA 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package org.onlyoffice.test;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Small helpers shared by the tests.
 */
public final class TestHelper {
    private TestHelper() {
    }

    public static DocumentModel createFile(final CoreSession session, final String name, final String fileName,
                                           final String mimeType, final String content) {
        DocumentModel model = session.createDocumentModel("/", name, "File");
        model.setPropertyValue("dc:title", name);

        if (fileName != null) {
            Blob blob = Blobs.createBlob(content, mimeType, StandardCharsets.UTF_8.name(), fileName);
            model.setPropertyValue("file:content", (Serializable) blob);
        }

        model = session.createDocument(model);
        session.save();
        return model;
    }

    public static void grant(final CoreSession session, final DocumentModel model, final String user,
                             final String permission) {
        ACP acp = model.getACP() != null ? model.getACP() : new ACPImpl();
        ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
        acl.add(new ACE(user, permission, true));
        session.setACP(model.getRef(), acp, true);
        session.save();
    }

    /**
     * Sets Nuxeo framework properties and restores the previous values on {@link #close()}.
     */
    public static final class FrameworkProperties implements AutoCloseable {
        private final Map<String, Object> originals = new HashMap<>();

        public FrameworkProperties set(final String name, final String value) {
            Properties properties = Framework.getProperties();
            if (!originals.containsKey(name)) {
                originals.put(name, properties.get(name));
            }
            if (value == null) {
                properties.remove(name);
            } else {
                properties.put(name, value);
            }
            return this;
        }

        @Override
        public void close() {
            Properties properties = Framework.getProperties();
            for (Map.Entry<String, Object> original : originals.entrySet()) {
                if (original.getValue() == null) {
                    properties.remove(original.getKey());
                } else {
                    properties.put(original.getKey(), original.getValue());
                }
            }
        }
    }

    /**
     * Replaces private fields (also declared in super classes) and restores the original values on {@link #close()}.
     * Services built by the plugin capture their collaborators in their constructors, so mocking them through the
     * service registry alone is not enough.
     */
    public static final class FieldOverrides implements AutoCloseable {
        private final Deque<Object[]> originals = new ArrayDeque<>();

        public FieldOverrides set(final Object target, final String fieldName, final Object value) {
            try {
                Field field = findField(target.getClass(), fieldName);
                field.setAccessible(true);
                originals.push(new Object[] {target, field, field.get(target)});
                field.set(target, value);
                return this;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void close() {
            while (!originals.isEmpty()) {
                Object[] original = originals.pop();
                try {
                    ((Field) original[1]).set(original[0], original[2]);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        private static Field findField(final Class<?> type, final String fieldName) throws NoSuchFieldException {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                try {
                    return current.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    // look in the super class
                }
            }
            throw new NoSuchFieldException(fieldName);
        }
    }
}
