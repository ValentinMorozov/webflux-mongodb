package com.example.mongoReactive.service;

import com.example.mongoReactive.util.ConvertDataException;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.example.mongoReactive.util.DocumentBuilder.*;

/**
 *
 * @author Валентин Морозов
 */
@Service
public class ChecksService {

    private static final Logger LOG = LoggerFactory.getLogger(ChecksService.class);
    /**
     * Преобразует чек из формата XML в объект типа bson.Document
     * @return объект запроса
     */
    public Document xml2Document(String paymentXML, String paymentFileName,  Document template)
            throws ConvertDataException, IOException {
        Document result = new Document();
        DocumentNode rootNode = createDocumentNode(null, "", result);
        DocumentNode checkNode = createDocumentNode(rootNode, "check");
        DocumentNode checkReceiptNode = createDocumentNode(checkNode, "receipt");
        DocumentNode checkReceiptClientNode = createDocumentNode(checkReceiptNode, "client");
        ValueReceiver checkReceiptClientReceiver = (Context ctx) -> {
            String key = ctx.getKey();
            if("email".equals(ctx.getKey()))
                checkReceiptClientNode.append(key, String::valueOf,  ctx);
            else
                LOG.info(buildMsgInfo("Unknown attribute", ctx));
            return null;
        };

        DocumentNode checkReceiptCompanyNode = createDocumentNode(checkReceiptNode, "company");
        ValueReceiver checkReceiptCompanyReceiver = (Context ctx) -> {
            String key = ctx.getKey();
            switch(key) {
                case "email":
                case "sno":
                case "inn":
                case "payment_address": checkReceiptCompanyNode.append(key, String::valueOf, ctx);
                    break;
                default: LOG.info(buildMsgInfo("Unknown attribute", ctx));
            }
            return null;
        };

        DocumentNode checkReceiptItemsNode = createDocumentNode(checkReceiptNode, "items", true);
        DocumentNode checkReceiptItemsItemNode = createDocumentNode(checkReceiptItemsNode, "item");
        DocumentNode checkReceiptItemsItemVatNode = createDocumentNode(checkReceiptItemsItemNode, "vat");
        ValueReceiver checkReceiptItemsItemVatReceiver = typeSumReceiver(checkReceiptItemsItemVatNode);

        ValueReceiver checkReceiptItemsItemReceiver = (Context ctx) -> {
            ValueReceiver receiver = null;
            String key = ctx.getKey();
            switch(key) {
                case "price":
                case "quantity":
                case "sum": checkReceiptItemsItemNode.append(key, Double::parseDouble, ctx);
                    break;
                case "name":
                case "measurement_unit":
                case "payment_method":
                case "payment_object": checkReceiptItemsItemNode.append(key, String::valueOf, ctx);
                    break;
                case "vat": receiver = testAndGetReceiver(checkReceiptItemsItemVatReceiver, ctx); break;
                default: LOG.info(buildMsgInfo("Unknown attribute", ctx));
            }
            return receiver;
        };
        ValueReceiver checkReceiptItemsItemArrayReceiver = arrayReceiver(checkReceiptItemsItemNode, checkReceiptItemsItemReceiver);

        ValueReceiver checkReceiptItemsReceiver = (Context ctx) -> {
            ValueReceiver receiver = null;
            String key = ctx.getKey();
            if("item".equals(key))
                receiver = testAndGetReceiver(ctx.getJsonValue().isObject() ?
                            checkReceiptItemsItemReceiver : checkReceiptItemsItemArrayReceiver, ctx);
            else
                LOG.info(buildMsgInfo("Unknown attribute", ctx));
            return receiver;
        };

        DocumentNode checkReceiptPaymentsNode = createDocumentNode(checkReceiptNode, "payments", true);
        DocumentNode checkReceiptPaymentsPaymentNode = createDocumentNode(checkReceiptPaymentsNode, "payment");
        ValueReceiver checkReceiptPaymentsPaymentReceiver = typeSumReceiver(checkReceiptPaymentsPaymentNode);
        ValueReceiver checkReceiptPaymentsArrayReceiver = arrayReceiver(checkReceiptPaymentsPaymentNode, checkReceiptPaymentsPaymentReceiver);

        ValueReceiver checkReceiptPaymentsReceiver = (Context ctx) -> {
            ValueReceiver receiver = null;
            String key = ctx.getKey();
            if("payment".equals(key))
                receiver = testAndGetReceiver(ctx.getJsonValue().isObject() ?
                        checkReceiptPaymentsPaymentReceiver : checkReceiptPaymentsArrayReceiver, ctx);
            else
                LOG.info(buildMsgInfo("Unknown attribute", ctx));
            return receiver;
        };

        DocumentNode checkReceiptVatsNode = createDocumentNode(checkReceiptNode, "vats", true);
        DocumentNode checkReceiptVatsVatNode = createDocumentNode(checkReceiptVatsNode, "vat");
        ValueReceiver checkReceiptVatsVatReceiver = typeSumReceiver(checkReceiptVatsVatNode);
        ValueReceiver checkReceiptVatsVatArrayReceiver = arrayReceiver(checkReceiptVatsVatNode, checkReceiptVatsVatReceiver);

        ValueReceiver checkReceiptVatsReceiver = (Context ctx) -> {
            ValueReceiver receiver = null;
            String key = ctx.getKey();
            if("vat".equals(key))
                receiver = testAndGetReceiver(ctx.getJsonValue().isObject() ?
                        checkReceiptVatsVatReceiver : checkReceiptVatsVatArrayReceiver, ctx);
            else
                LOG.info(buildMsgInfo("Unknown attribute", ctx));
            return receiver;
        };

        ValueReceiver checkReceiptReceiver = (Context ctx) -> {
            ValueReceiver receiver = null;
            String key = ctx.getKey();
            switch(key) {
                case "client": receiver = testAndGetReceiver(checkReceiptClientReceiver, ctx); break;
                case "company": receiver = testAndGetReceiver(checkReceiptCompanyReceiver, ctx); break;
                case "items": receiver = testAndGetReceiver(checkReceiptItemsReceiver, ctx); break;
                case "payments": receiver = testAndGetReceiver(checkReceiptPaymentsReceiver, ctx); break;
                case "vats": receiver = testAndGetReceiver(checkReceiptVatsReceiver, ctx); break;
                case "total": checkReceiptNode.append(key, Double::parseDouble, ctx); break;
                case "operation":
                case "cashier": checkReceiptNode.append(key, String::valueOf, ctx);
                    break;
                default: LOG.info(buildMsgInfo("Unknown attribute", ctx));
            }
            return receiver;
        };
        ValueReceiver checkReceiver = (Context ctx) -> {
            ValueReceiver receiver = null;
            String key = ctx.getKey();
            switch(key) {
                case "timestamp":
                case "external_id": checkNode.append(key, String::valueOf, ctx);
                    break;
                case "is_bso": checkNode.append(key, Boolean::valueOf, ctx); break;
                case "receipt": receiver = testAndGetReceiver(checkReceiptReceiver, ctx); break;
                default: LOG.info(buildMsgInfo("Unknown attribute", ctx));
            }
            return receiver;
        };

        ValueReceiver rootReceiver = new ValueReceiver() {
            public ValueReceiver receive(Context ctx) {
                if(ctx.getJsonValue().isObject() && ctx.getKey().equals("check")) return checkReceiver;
                return this;
            }
        };
        try {
            forEachNode(XML2Node(paymentXML), paymentFileName, rootReceiver, null);
            appendNotExistsInDocument(template, ((Document) result.get("check")));
        }
        catch(ConvertDataException | IOException e) {
            LOG.warn(e.getMessage());
            throw e;
        }
        return result;
    }

    static private ValueReceiver typeSumReceiver(final DocumentNode documentNode) {
        return (Context ctx) -> {
            String key = ctx.getKey();
            switch(key) {
                case "type": documentNode.append(key, String::valueOf,  ctx); break;
                case "sum":  documentNode.append(key, Double::parseDouble,  ctx); break;
                default: LOG.info(buildMsgInfo("Unknown attribute", ctx));
            }
            return null;
        };
    }

    static private ValueReceiver arrayReceiver(final DocumentNode documentNode, final ValueReceiver newReceiver) {
        return (Context ctx) -> {
            documentNode.newNode();
            return testAndGetReceiver(newReceiver, ctx);
        };
    }
}
