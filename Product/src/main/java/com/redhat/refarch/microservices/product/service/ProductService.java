package com.redhat.refarch.microservices.product.service;

import com.redhat.refarch.microservices.product.model.Error;
import com.redhat.refarch.microservices.product.model.Inventory;
import com.redhat.refarch.microservices.product.model.Keyword;
import com.redhat.refarch.microservices.product.model.Product;
import com.redhat.refarch.microservices.utils.Utils;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/products")
@Stateless
@LocalBean
public class ProductService
{

	@PersistenceContext
	private EntityManager em;

	private Logger logger = Logger.getLogger( getClass().getName() );

	@Path("/")
	@POST
	@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public Product addProduct(Product product)
	{
		try
		{
			logInfo( "Will persist product " + product );
			em.persist( product );
			return product;
		}
		catch( RuntimeException e )
		{
			logError( "Got exception " + e.getMessage() );
			throw new Error( HttpURLConnection.HTTP_INTERNAL_ERROR, e ).asException();
		}
	}

	@GET
	@Path("/")
	@Produces({"application/json", "application/xml"})
	public Collection<Product> getProducts(@Context UriInfo uriInfo)
	{
		try
		{
			MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
			if( queryParams.containsKey( "featured" ) )
			{
				return em.createNamedQuery( "Product.findFeatured", Product.class ).getResultList();
			}
			else if( queryParams.containsKey( "keyword" ) )
			{
				Collection<Product> products = new HashSet<Product>();
				for( String keyword : queryParams.get( "keyword" ) )
				{
					try
					{
						TypedQuery<Keyword> query = em.createNamedQuery( "Keyword.findKeyword", Keyword.class );
						query.setParameter( "query", keyword );
						Keyword keywordEntity = query.getSingleResult();
						List<Product> keywordProducts = keywordEntity.getProducts();
						logInfo( "Found " + keyword + ": " + keywordProducts );
						products.addAll( keywordProducts );
					}
					catch( NoResultException e )
					{
						//keyword not found, which is acceptable
					}
				}
				return products;
			}
			else
			{
				throw new Error( HttpURLConnection.HTTP_BAD_REQUEST, "All products cannot be returned" ).asException();
			}
		}
		catch( WebApplicationException e )
		{
			throw e;
		}
		catch( RuntimeException e )
		{
			throw new Error( HttpURLConnection.HTTP_INTERNAL_ERROR, e ).asException();
		}
	}

	@GET
	@Path("/{sku}")
	@Produces({"application/json", "application/xml"})
	public Product getProduct(@PathParam("sku") Long sku)
	{
		try
		{
			logInfo( "SKU is " + sku );
			Product product = em.find( Product.class, sku );
			if( product == null )
			{
				throw new Error( HttpURLConnection.HTTP_NOT_FOUND, "Product not found" ).asException();
			}
			return product;
		}
		catch( RuntimeException e )
		{
			throw new Error( HttpURLConnection.HTTP_INTERNAL_ERROR, e ).asException();
		}
	}

	@PUT
	@Path("/{sku}")
	@Consumes({"application/json", "application/xml"})
	@Produces({"application/json", "application/xml"})
	public Product updateProduct(@PathParam("sku") Long sku, Product product)
	{
		Product entity = getProduct( sku );
		try
		{
			//Ignore any attempt to update product SKU:
			product.setSku( sku );
			Utils.copy( product, entity, false );
			em.merge( entity );
			return product;
		}
		catch( RuntimeException e )
		{
			throw new Error( HttpURLConnection.HTTP_INTERNAL_ERROR, e ).asException();
		}
	}

	@PATCH
	@Path("/{sku}")
	@Consumes({"application/json", "application/xml"})
	@Produces({"application/json", "application/xml"})
	public Product partiallyUpdateProduct(@PathParam("sku") Long sku, Product product)
	{
		Product entity = getProduct( sku );
		try
		{
			//Ignore any attempt to update product SKU:
			product.setSku( sku );
			Utils.copy( product, entity, true );
			em.merge( entity );
			return product;
		}
		catch( RuntimeException e )
		{
			throw new Error( HttpURLConnection.HTTP_INTERNAL_ERROR, e ).asException();
		}
	}

	@DELETE
	@Path("/{sku}")
	@Consumes({"application/json", "application/xml"})
	@Produces({"application/json", "application/xml"})
	public void deleteProduct(@PathParam("sku") Long sku)
	{
		Product product = getProduct( sku );
		try
		{
			em.remove( product );
		}
		catch( RuntimeException e )
		{
			throw new Error( HttpURLConnection.HTTP_INTERNAL_ERROR, e ).asException();
		}
	}

	@Path("/keywords")
	@POST
	@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public Keyword addKeyword(Keyword keyword)
	{
		try
		{
			logInfo( "Will persist keyword " + keyword );
			em.persist( keyword );
			return keyword;
		}
		catch( RuntimeException e )
		{
			logError( "Got exception " + e.getMessage() );
			throw new Error( HttpURLConnection.HTTP_INTERNAL_ERROR, e ).asException();
		}
	}

	@POST
	@Path("/classify/{sku}")
	@Consumes({"application/json", "application/xml"})
	@Produces({"application/json", "application/xml"})
	public void classifyProduct(@PathParam("sku") Long sku, List<Keyword> keywords)
	{
		Product product = getProduct( sku );
		logInfo( "Asked to classify " + product + " as " + keywords );
		try
		{
			product.setKeywords( keywords );
			em.merge( product );
		}
		catch( RuntimeException e )
		{
			throw new Error( HttpURLConnection.HTTP_INTERNAL_ERROR, e ).asException();
		}
	}

	@POST
	@Path("/reduce")
	@Consumes({"application/json", "application/xml"})
	@Produces({"application/json", "application/xml"})
	public Response reduceInventory(Inventory[] inventoryAdjustment)
	{
		try
		{
			logInfo( "Asked to reduce inventory: " + Arrays.toString( inventoryAdjustment ) );
			for( Inventory inventory : inventoryAdjustment )
			{
				Product product = em.find( Product.class, inventory.getSku(), LockModeType.PESSIMISTIC_WRITE );
				logInfo( "Looked up product as " + product );
				if( product == null )
				{
					throw new Error( HttpURLConnection.HTTP_NOT_FOUND, "Product not found" ).asException();
				}
				int availability = product.getAvailability();
				if( inventory.getQuantity() > availability )
				{
					String message = "Insufficient availability for " + inventory.getSku();
					throw new Error( HttpURLConnection.HTTP_CONFLICT, message ).asException();
				}
				else
				{
					product.setAvailability( availability - inventory.getQuantity() );
					em.merge( product );
					logInfo( "Saved " + product );
				}
			}
		}
		catch( RuntimeException e )
		{
			throw new Error( HttpURLConnection.HTTP_INTERNAL_ERROR, e ).asException();
		}
		return Response.ok().build();
	}

	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@HttpMethod("PATCH")
	public @interface PATCH
	{
	}

	private void logInfo(String message)
	{
		logger.log( Level.INFO, message );
	}

	private void logError(String message)
	{
		logger.log( Level.SEVERE, message );
	}
}