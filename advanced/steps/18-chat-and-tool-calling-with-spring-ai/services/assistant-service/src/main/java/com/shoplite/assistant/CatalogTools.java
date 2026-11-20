package com.shoplite.assistant;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.shoplite.assistant.CatalogClient.ProductDetails;
import com.shoplite.assistant.CatalogClient.ProductHit;

/**
 * The tools the model may call. Spring AI sends each method's name, description and parameters
 * to the model; when the model asks for a call, Spring AI runs the method and sends the result back.
 * Both tools only read public catalog data, so a confused or manipulated model cannot change anything.
 */
@Component
public class CatalogTools {

    static final int MAX_RESULTS = 5;

    private static final Logger log = LoggerFactory.getLogger(CatalogTools.class);

    private final CatalogClient catalog;

    public CatalogTools(CatalogClient catalog) {
        this.catalog = catalog;
    }

    @Tool(description = "Search the ShopLite catalog by words in the product name. "
            + "Returns at most 5 products with id, name, price in EUR, stock and category.")
    public List<ProductHit> searchProducts(
            @ToolParam(description = "One or two words to look for, for example 'mug' or 'desk lamp'") String query) {
        log.info("Tool call: searchProducts(\"{}\")", query);
        return catalog.search(query, MAX_RESULTS).content();
    }

    @Tool(description = "Get the full details of one product, including its description, by product id.")
    public ProductDetails productDetails(@ToolParam(description = "The product id from searchProducts") long productId) {
        log.info("Tool call: productDetails({})", productId);
        try {
            return catalog.product(productId);
        }
        catch (HttpClientErrorException.NotFound ex) {
            // A null result tells the model that the product does not exist, instead of failing the chat.
            return null;
        }
    }

}
