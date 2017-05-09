package com.redhat.refarch.microservices.trigger.model;

import java.math.BigDecimal;
import java.util.List;

public class Product
{

	private Long sku;
	private String name;
	private String description;
	private Integer length;
	private Integer width;
	private Integer height;
	private Integer weight;
	private Boolean featured;
	private Integer availability;
	private BigDecimal price;
	private String image;

	private List<Keyword> keywords;

	public Long getSku()
	{
		return sku;
	}

	public void setSku(Long sku)
	{
		this.sku = sku;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public Integer getLength()
	{
		return length;
	}

	public void setLength(Integer length)
	{
		this.length = length;
	}

	public Integer getWidth()
	{
		return width;
	}

	public void setWidth(Integer width)
	{
		this.width = width;
	}

	public Integer getHeight()
	{
		return height;
	}

	public void setHeight(Integer height)
	{
		this.height = height;
	}

	public Integer getWeight()
	{
		return weight;
	}

	public void setWeight(Integer weight)
	{
		this.weight = weight;
	}

	public Boolean getFeatured()
	{
		return featured;
	}

	public void setFeatured(Boolean featured)
	{
		this.featured = featured;
	}

	public Integer getAvailability()
	{
		return availability;
	}

	public void setAvailability(Integer availability)
	{
		this.availability = availability;
	}

	public BigDecimal getPrice()
	{
		return price;
	}

	public void setPrice(BigDecimal price)
	{
		this.price = price;
	}

	public String getImage()
	{
		return image;
	}

	public void setImage(String image)
	{
		this.image = image;
	}

	public void setKeywords(List<Keyword> keywords)
	{
		this.keywords = keywords;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( sku == null ) ? 0 : sku.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if( this == obj )
			return true;
		if( obj == null )
			return false;
		if( getClass() != obj.getClass() )
			return false;
		Product other = (Product)obj;
		if( sku == null )
		{
			if( other.sku != null )
				return false;
		}
		else if( !sku.equals( other.sku ) )
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "Product [sku=" + sku + ", name=" + name + ", description=" + description + ", length=" + length + ", width=" + width + ", height="
				+ height + ", weight=" + weight + ", featured=" + featured + ", availability=" + availability + ", price=" + price + ", image="
				+ image + "]";
	}
}