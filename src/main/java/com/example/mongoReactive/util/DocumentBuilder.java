package com.example.mongoReactive.util;

import lombok.*;
import org.bson.Document;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Класс DocumentBuilder представляет набор классов, интепфейсов и методов для обработки и формирования дерева документа.
 *
 * @author Морозов Валентин
 */
public class DocumentBuilder {
    final private static XmlMapper xmlMapper = new XmlMapper();
//
    @FunctionalInterface
    public interface ValueReceiver {
        ValueReceiver receive(Context ctx) throws ConvertDataException ;
    }

//
    @FunctionalInterface
    public interface ValueConverter {
        Object convert(String value);
    }
    /**
     * Класс Context объединяет множества параметров в один объект, используется для минимизации количества передаваемы
     * параметров при вызове методов.
     *
     */
    @Getter
    public static class Context  {
        /** Переменная для хранения имени обрабатываемого файла(объекта). */
        final private String fileName;
        /** Переменная для хранения объекта ресивера, может использоваться ресиверами для хранения данных
         * используемых/формируемых в поцессе обработки. */
        @Setter(AccessLevel.PACKAGE)
        private Object receiverObject;
        /** Переменная для хранения ключа атрибута. */
        private String key;
        /** Переменная для хранения значения атрибута. */
        private Object value;
        /** Переменная для хранения текущего пути к атрибуту. */
        final private Stack<String> path;
        /** Переменная для хранения объекта обработки исключений **/
        final private Predicate<Exception> onError;
        Context(Object receiverObject, String fileName) {
            this(receiverObject, fileName, (Exception e) -> false);
        }
        Context(Object receiverObject, String fileName, Predicate<Exception> onError) {
            this.receiverObject = receiverObject;
            this.fileName = fileName;
            this.path = new Stack<>();
            this.onError = onError;
        }
        public Context setKeyValue(String key, Object value) {
            this.key = key;
            this.value = value;
            return this;
        }

        public JsonNode getJsonValue() {
            return (value instanceof JsonNode) ? (JsonNode)value : null;
        }

        public Document getDocumentValue() {
            return (value instanceof Document) ? (Document)value : null;
        }

        public String push(String key) {
            return path.push(key);
        }

        public String pop() {
            return path.pop();
        }
    }

    /**
     * Класс DocumentNode представляет узел формируемого дерева Document.
     * Узел может содержать объект типа Document или
     * ArarayList. Узел может формироваться как статически, задаётся при создании объекта, так и динамически,
     * формируется в случае наличия атрибутов в дочерних узлах.
     *
     */
    @AllArgsConstructor
    @Getter
    public static class DocumentNode  {
        /** Переменная для хранения ссылки на родительский документ. */
        final private DocumentNode parentDocumentNode;
        /** Переменная для хранения ключа узла. */
        final private String key;
        /** Переменная для хранения узла документа. */
        @Setter
        private Object document;
        /** Переменная для хранения признака, что данный узел является массивом. */
        final private boolean isArray;
        /**
         * Добавляет элемент в узел, если узел является массивом. Если объект массива не существовал - то он создаётся.
         * @param item добавляемый элемент
         * @return индекс в массиве, добавленного элемента
         */
        public boolean addIsArray(Object item) {
            if(isArray()) {
                ArrayList<Object> array = isArray() ? (ArrayList<Object>) document : null;
                if(isNull(array)) {
                    array = new ArrayList<>();
                    document = array;
                }
                if(array.isEmpty() || array.get(array.size() - 1) != item) {
                    array.add(item);
                }
                return true;
            }
            return false;
        }

        boolean isArray() {
            return isArray;
        }
        /**
         * Выполняет преобразование значения, передаваемого в ctx и добавляет его в узел.
         * @param key ключ, добавляемого значения
         * @param converter функция преобразования значения
         * @param ctx набор параметров
         * @return истина, если значение добавлено
         */
        public boolean append(String key, ValueConverter converter, Context ctx) throws ConvertDataException {
            return append(key, (ctx.getValue() instanceof JsonNode) ?
                    convertValueJsonNode(converter, ctx) : convertValue(converter, ctx));
        }
        /**
         * Добавляет атрибут в узел. .
         * @param key ключ
         * @param value значение
         * @return истина, если значение добавлено
         */
        public boolean append(String key, Object value) {
            boolean rc = false;
            if(nonNull(value)) {
                rc = createDocumentNode(null);
                if (rc) {
                    if(isArray())
                        ((ArrayList)document).add(value);
                    else
                        ((Document)document).append(key, value);
                }
            }
            return rc;
        }
        /**
         * Проверяет наличие объекта узла, d случае отсутствия создаёт его и добавляет в него объект дочернего узела.
         * Вызывает цепочку изменений в дереве документов.
         * @param children дочерний узел, обрабатывающи добавление атрибута
         * @return истина, если узел создан без ошибок
         */
        public boolean createDocumentNode(DocumentNode children) {
            if(isNull(document)) {
                document = isArray() ? new ArrayList<>() : new Document();
            }
            if(nonNull(children)) {
                if(!addIsArray(children.getDocument()))
                    ((Document)document).append(children.getKey(), children.getDocument());
            }
            boolean rc = true;
            if(nonNull(parentDocumentNode))
                rc = rc & parentDocumentNode.createDocumentNode(this);
            return rc;
        }

        public boolean newNode() {
            document = isArray() ? new ArrayList<>() : new Document();
            return parentDocumentNode.createDocumentNode(this);
        }
        //
    }
    /**
     * Набор методов создания узла документов.
     * @return созданный объект DocumentNode
     */
    public static DocumentNode createDocumentNode(DocumentNode parentDocumentNode, String key, Document document) {
        return new DocumentNode(parentDocumentNode, key, document, false);
    }
    public static DocumentNode createDocumentNode(DocumentNode parentDocumentNode, String key) {
        return new DocumentNode(parentDocumentNode, key, null, false);
    }
    public static DocumentNode createDocumentNode(DocumentNode parentDocumentNode, String key,  boolean isArray) {
        return new DocumentNode(parentDocumentNode, key, null, isArray);
    }
    /**
     * Преобразует документ в формате XML в формат JsonNode. В случае ошибки преобразования пишет в лог сообщение об ошибке
     * @param documentXML документ в формате XML
     * @return распарсенный объект
     */
    public static JsonNode XML2Node(String documentXML) throws IOException {
        return xmlMapper.readTree(documentXML);
    }
    /**
     * Запускает процесс обработки дерева JsonNode. Для каждого элемента вызывается метод receive интерфейса ValueReceiver.
     * Метод receive возвращает null или новый объект, реализующий интерфейс ValueReceiver, в котором реализована
     * приёмка атрибута. null означает, что дальнейшая обработка атрибута не требуется.
     * @param rootNode вершина дернева документа
     * @param documentName имя документа
     * @param receiver объект, принимающий элементы дерева документа
     * @param receiverObject необязательный параметр, передаётся в наборе параметров при вызове метода receive интерфейса
     *                       ValueReceiver.
     * @param onError  необязательный параметр, обработчик ошибок. Вызывается при возникновении ошибок. Если возвращает "true"
     *                 ошибка считается обработанной и сключение не генерируется.
     * @return объект, переданный в качестве параметра receiverObject
     */
    public static Object forEachNode(JsonNode rootNode, String documentName, ValueReceiver receiver, Object receiverObject, Predicate<Exception> onError)
            throws ConvertDataException {
        if(nonNull(rootNode)) {
            Context ctx = new Context(receiverObject, documentName, onError);
            processingValueJsonNode(receiver, ctx.setKeyValue("", rootNode));
            return ctx.getReceiverObject();
        }
        return null;
    }
    public static Object forEachNode(JsonNode rootNode, String documentName, ValueReceiver receiver, Object receiverObject)
            throws ConvertDataException {
        if(nonNull(rootNode)) {
            Context ctx = new Context(receiverObject, documentName);
            processingValueJsonNode(receiver, ctx.setKeyValue("", rootNode));
            return ctx.getReceiverObject();
        }
        return null;
    }
    //
    /**
     * Перебирает элементы дерева JsonNode. Для каждого элемента вызывается метод receive интерфейса ValueReceiver.
     * Метод receive возвращает null или новый объект, реализующий интерфейс ValueReceiver, в котором реализована
     * приёмка атрибута. null означает, что дальнейшая обработка атрибута не требуется.
     * @param receiver объект, принимающий элементы дерева документа
     * @param ctx набор параметров
     */
    private static void processingValueJsonNode(ValueReceiver receiver, Context ctx) throws ConvertDataException  {
        if(nonNull(receiver)) {
            String keyValue = ctx.getKey();
            JsonNode value = (JsonNode)ctx.getValue();
            ValueReceiver valueReceiver = receiver.receive(ctx.setKeyValue(keyValue, value));
            ctx.push(keyValue);
            if (value.isObject()) {
                for (Iterator<Map.Entry<String,JsonNode>> fields = value.fields(); fields.hasNext(); ) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    processingValueJsonNode(valueReceiver, ctx.setKeyValue(field.getKey(), field.getValue()));
                }
            } else if (value.isArray()) {
                for(int i = 0; i < value.size(); i++) {
                    processingValueJsonNode(valueReceiver, ctx.setKeyValue(String.valueOf(i), value.get(i)));
                }
            }
            ctx.pop();
        }
    }
    /**
     * Возвращает истину если объект не является конечным атрибутом.
     * @param value объект элемента дерева
     * @return истина, если параметр не является узлом
     */
    public static boolean nonValueNode(Object value) {
        return ((value instanceof Document) | (value instanceof List));
    }
    /**
     * Возвращает истину если объект является конечным атрибутом.
     * @param value объект элемента дерева
     * @return истина, если параметр является узлом
     */
    public static boolean isValueNode(Object value) {
        return !nonValueNode(value);
    }
    /**
     * Создаёт и возвращает новый узел документа аналогичный типу параметра value для окумента и списка.
     * Для конечного атрибута возвращает объект самого атрибута
     * @param value объект элемента дерева
     * @return созданный объект или параметр value
     */
    public static Object createNode(Object value) {
        return  (value instanceof Document) ? new Document() : (
                (value instanceof ArrayList) ? new ArrayList<>() : value);
    }
    /**
     * Добавляет объект в целевой объект Document или ArrayList, помещает в стек пути добавленный объект.
     * @param node добавляемый узел/объект
     * @param target целевой объект
     * @param key ключ
     * @param pathInTree путь в дереве до объекта
     * @return истина, если узел помещен стек pathInTree
     */
    public static boolean addNode(Object node, Object target, String key, Stack<Object> pathInTree) {
        boolean rc = true;
        if(target instanceof Document) ((Document)target).append(key, node);
        else if(target instanceof ArrayList)  ((ArrayList)target).add(node);
        else rc = false;
        if(node instanceof Document || node instanceof ArrayList) {
            pathInTree.push(node);
        }
        return rc;
    }
    /**
     * Синхронизирует стек пути в документе со стеком ключей, возвращает соответствующий пути объект из стека .
     * @param pathDocs путь в документе
     * @param pathKeys путь ключей
     * @return текущий узел из стека параметра pathDocs
     */
    public static Object getTarget(Stack<Object> pathDocs, Stack<String> pathKeys)
    {
        while(pathDocs.size() > pathKeys.size()) {
            pathDocs.pop();
        }
        return pathDocs.peek();
    }
    /**
     * Запускает процесс обработки дерева Document. Для каждого элемента вызывается метод receive интерфейса ValueReceiver.
     * Метод receive возвращает null или новый объект, реализующий интерфейс ValueReceiver, в котором реализована
     * приёмка атрибута. null означает, что дальнейшая обработка атрибута не требуется.
     * @param rootNode вершина дернева документа
     * @param documentName имя документа
     * @param receiver объект, принимающий элементы дерева документа
     * @param receiverObject необязательный параметр, передаётся в наборе параметров при вызове метода receive интерфейса
     *                       ValueReceiver.
     * @return объект, переданный в качестве параметра receiverObject
     */
    public static Object forEachNode(Document rootNode, String documentName, ValueReceiver receiver, Object receiverObject)
            throws ConvertDataException {
        if(nonNull(rootNode)) {
            Context ctx = new Context(receiverObject, documentName);
            processingValue(receiver, ctx.setKeyValue("", rootNode));
            return ctx.getReceiverObject();
        }
        return null;
    }
    /**
     * Перебирает элементы дерева Document. Для каждого элемента вызывается метод receive интерфейса ValueReceiver.
     * Метод receive возвращает null или новый объект, реализующий интерфейс ValueReceiver, в котором реализована
     * приёмка атрибута. null означает, что дальнейшая обработка атрибута не требуется.
     * @param receiver объект, принимающий элементы дерева документа
     * @param ctx набор параметров
     */
    private static void processingValue(ValueReceiver receiver, Context ctx) throws ConvertDataException {
        if(nonNull(receiver)) {
            String keyValue = ctx.getKey();
            Object value = ctx.getValue();
            ValueReceiver valueReceiver = receiver.receive(ctx.setKeyValue(keyValue, value));
            ctx.push(keyValue);
            if (value instanceof Document) {
                for (Map.Entry<String,Object> node : ((Document) value).entrySet()) {
                    processingValue(valueReceiver, ctx.setKeyValue(node.getKey(), node.getValue()));
                }
            } else if (value instanceof List) {
                int i = 0;
                for(Object item: (List<Object>) value) {
                    processingValue(valueReceiver, ctx.setKeyValue(String.valueOf(i++), item));
                }
            }
            ctx.pop();
        }
    }
    /**
     * Преобразует значение атрибута JsonNode в значение типа в соответствии с методом преобразования.
     * @param converter объект, принимающий элементы дерева документа
     * @param ctx набор параметров
     * @return объект, содержащий значение заданного типа
     */
    public static Object convertValueJsonNode(ValueConverter converter, Context ctx) throws ConvertDataException {
        Object value = null;
        if(ctx.getJsonValue().isValueNode()) {
            try {
                value = converter.convert(ctx.getJsonValue().asText());
            }
            catch (Exception e) {
                ConvertDataException exception = new ConvertDataException(buildMsgInfo("Convert:", ctx), e);
                if(!ctx.onError.test(exception))
                    throw exception;
            }
        }
        else {
            ConvertDataException exception = new ConvertDataException(buildMsgInfo("Type is not an value node:", ctx));
            if(!ctx.onError.test(exception))
                throw exception;
        }
        return value;
    }
    /**
     * Преобразует значение атрибута Document в значение типа в соответствии с методом преобразования.
     * @param converter объект, принимающий элементы дерева документа
     * @param ctx набор параметров
     * @return объект, содержащий значение заданного типа
     */
    public static Object convertValue(ValueConverter converter, Context ctx) throws ConvertDataException {
        Object value = null;
        Object valueNode = ctx.getValue();
        try {
            value = nonNull(converter) && valueNode instanceof String ? converter.convert((String)valueNode) : valueNode;
        }
        catch (Exception e) {
            ConvertDataException exception = new ConvertDataException(buildMsgInfo("Convert:", ctx), e);
            if(!ctx.onError.test(exception))
                throw exception;
        }
        return value;
    }
    /**
     * Проверяет тип объекта и возвращает receiver или null, если объект не может иметь собственный ресивер.
     * @param receiver объект, принимающий элементы дерева документа
     * @param ctx набор параметров
     * @return объект, принимающий документ
     */
    public static ValueReceiver testAndGetReceiver(ValueReceiver receiver, Context ctx) throws ConvertDataException {
        if(ctx.getJsonValue().isObject() || ctx.getJsonValue().isArray()) return receiver;
        else {
            ConvertDataException exception = new ConvertDataException(buildMsgInfo("Type is not an value node:", ctx));
            if(!ctx.onError.test(exception))
                throw exception;
        }
        return null;
    }
    /**
     * Генерирует текст сообщения.
     * @param textMsg объект, принимающий элементы дерева документа
     * @param ctx набор параметров
     * @return текст сообщения
     */
    public static String buildMsgInfo(String textMsg, Context ctx) {
        return textMsg + " " +
                "\"" + ctx.getKey() + "\"" +
                " path " + ctx.getPath().stream().collect(Collectors.joining("\\", "", "\\")) +
                " in " + ctx.getFileName();
    }
    /**
     * Добавляет в целевой документ отсутствующие в нем атрибуты.
     * @param sourceDocument исходный документ
     * @param targetDocument целевой документ
     * @return модифицируемый документ
     */
    public static Document appendNotExistsInDocument(Document sourceDocument, Document targetDocument)
            throws ConvertDataException{
        Stack<Object> pathInTree = new Stack();
        int indexCopy[] = {-1}; // Переменная в виде массива для возможности модификации внутри замыкания
        ValueReceiver appendTemplateReceiver = new ValueReceiver() {
            public ValueReceiver receive(Context ctx) {
                String key = ctx.getKey();
                Object value = ctx.getValue();
                Stack<String> pathDocs = ctx.getPath();
                Object target = getTarget(pathInTree, ctx.getPath());
                Object node = null;
                if(pathDocs.size() <= indexCopy[0]) { // Поднялись в дереве выше точки копирования
                    indexCopy[0] = -1;
                }
                if(indexCopy[0] == -1) {
                    if(target instanceof Document) {
                        node = ((Document) target).get(key);
                        if (isNull(node)) {
                            indexCopy[0] = pathInTree.size();
                        }
                    } else if(target instanceof List) {
                        node = ((ArrayList<Object>) target).get(Integer.parseInt(key));
                        if (isNull(node)) {
                            indexCopy[0] = pathInTree.size();
                        }
                    }
                }
                if (isNull(node)) {
                    node = createNode(value);
                    addNode(node, target, key, pathInTree);
                } else {
                    pathInTree.push(node);
                }
                return this;
            }
        };
        ValueReceiver appendTemplateReceiverRoot = createRootReceiver(appendTemplateReceiver, pathInTree);
        return (Document)forEachNode(sourceDocument, "", appendTemplateReceiverRoot, targetDocument);
    }
    /**
     * Саздаёт ресивер для копирования.
     * @param nextReceiver следующий ресивер
     * @param pathInTree путь в дереве
     * @return объект, принимающий данные обрабатываемого дерева
     */
    public static ValueReceiver createRootReceiver(ValueReceiver nextReceiver, Stack<Object> pathInTree) {
        ValueReceiver receiver = ctx -> {
            Object root = ctx.getReceiverObject();
            if(isNull(root)) {
                root = createNode(ctx.getValue());
            }
            pathInTree.push(root);
            ctx.setReceiverObject(root);
            return nextReceiver;
        };
        return receiver;
    }
}
