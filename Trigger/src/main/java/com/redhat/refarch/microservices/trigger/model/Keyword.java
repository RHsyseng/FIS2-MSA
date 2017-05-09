package com.redhat.refarch.microservices.trigger.model;

import java.util.List;

public class Keyword
{

	private String keyword;

	private List<Product> products;

	public String getKeyword()
	{
		return keyword;
	}

	public void setKeyword(String keyword)
	{
		this.keyword = keyword;
	}

	public List<Product> getProducts()
	{
		return products;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( keyword == null ) ? 0 : keyword.hashCode() );
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
		Keyword other = (Keyword)obj;
		if( keyword == null )
		{
			if( other.keyword != null )
				return false;
		}
		else if( !keyword.equals( other.keyword ) )
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "Keyword [keyword=" + keyword + "]";
	}
}