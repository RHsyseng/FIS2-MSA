package com.redhat.refarch.ecom.model

import groovy.transform.EqualsAndHashCode
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.springframework.data.annotation.Id

import java.math.RoundingMode

@EqualsAndHashCode
@ApiModel(description = "Represents a product in the system")
class Product {

    @Id
    @ApiModelProperty(value = "Persistence ID of product", required = false)
    String sku

    @ApiModelProperty(value = "Name of product", required = true)
    String name

    @ApiModelProperty(value = "Description of product", required = true)
    String description

    @ApiModelProperty(value = "Imperial length dimension of product", required = true)
    Double length

    @ApiModelProperty(value = "Imperial width dimension of product", required = true)
    Double width

    @ApiModelProperty(value = "Imperial height dimension of product", required = true)
    Double height

    @ApiModelProperty(value = "Imperial weight measurement of product", required = true)
    Double weight

    @ApiModelProperty(value = "Flag indicating if product is to be specially advertised as featured", required = true)
    Boolean isFeatured

    @ApiModelProperty(value = "Quantity of product available for purchase", required = true)
    Integer availability

    @ApiModelProperty(value = "Price of a single product unit", required = true)
    BigDecimal price

    @ApiModelProperty(value = "Visual representation identifier for product", required = true)
    String image

    @ApiModelProperty(value = "Keywords which categorize the product", required = true)
    List<String> keywords = []

    @ApiModelProperty(value = "Taglines to be shown for product detail display", required = true)
    List<String> taglines = []

    Product() {}

    Product(String sku, String name, String description, Double length, Double width, Double height, Double weight,
            Boolean isFeatured, Integer availability, BigDecimal price, String image, List<String> keywords,
            List<String> taglines) {
        this.sku = sku
        this.name = name
        this.description = description
        this.length = length
        this.width = width
        this.height = height
        this.weight = weight
        this.isFeatured = isFeatured
        this.availability = availability
        this.price = price
        this.image = image
        this.keywords = keywords
        this.taglines = taglines
    }

    @Override
    String toString() {
        return "Product{sku='${sku}', name='${name}', description='${description}', length=${length}, " +
                "width=${width}, height=${height}, weight=${weight}, isFeatured=${isFeatured}, " +
                "availability=${availability}, price=${price}, image='${image}', keywords=${keywords}, taglines=${taglines}"
    }


    static class ProductBuilder {

        String sku
        String name
        String description
        Double length
        Double width
        Double height
        Double weight
        Boolean isFeatured
        Integer availability
        BigDecimal price
        String image
        List<String> keywords = []
        List<String> taglines = []

        ProductBuilder sku(String sku) {
            this.sku = sku
            return this
        }

        ProductBuilder name(String name) {
            this.name = name
            return this
        }

        ProductBuilder description(String description) {
            this.description = description
            return this
        }

        ProductBuilder length(Double length) {
            this.length = length
            return this
        }

        ProductBuilder width(Double width) {
            this.width = width
            return this
        }

        ProductBuilder height(Double height) {
            this.height = height
            return this
        }

        ProductBuilder weight(Double weight) {
            this.weight = weight
            return this
        }

        ProductBuilder featured(Boolean featured) {
            this.isFeatured = featured
            return this
        }

        ProductBuilder availability(Integer availability) {
            this.availability = availability
            return this
        }

        ProductBuilder price(BigDecimal price) {
            this.price = price.setScale(2, RoundingMode.CEILING)
            return this
        }

        ProductBuilder image(String image) {
            this.image = image
            return this
        }

        ProductBuilder keywords(List<String> keywords) {
            this.keywords = keywords
            return this
        }

        ProductBuilder taglines(List<String> taglines) {
            this.taglines = taglines
            return this
        }

        Product build() {
            return new Product(sku, name, description, length, width, height, weight, isFeatured, availability, price,
                    image, keywords, taglines)
        }
    }
}