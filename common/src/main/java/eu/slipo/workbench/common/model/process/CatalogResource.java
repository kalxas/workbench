package eu.slipo.workbench.common.model.process;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vividsolutions.jts.geom.Geometry;

import eu.slipo.workbench.common.model.poi.EnumResourceType;
import eu.slipo.workbench.common.model.resource.ResourceIdentifier;

/**
 * A process input resource that already exists in the catalog
 */
public class CatalogResource extends ProcessInput
{
    private static final long serialVersionUID = 1L;

    private ResourceIdentifier resource;

    private String description;

    private UUID tableName;

    private Geometry boundingBox;

    protected CatalogResource()
    {
        super(-1, EnumInputType.CATALOG, null);
    }

    protected CatalogResource(
        int key, String name, EnumResourceType resourceType, ResourceIdentifier resourceIdentifier)
    {
        super(key, EnumInputType.CATALOG, name, resourceType);
        this.resource = new ResourceIdentifier(resourceIdentifier);
    }

    protected CatalogResource(int key, String name, ResourceIdentifier resourceIdentifier)
    {
        super(key, EnumInputType.CATALOG, name);
        this.resource = new ResourceIdentifier(resourceIdentifier);
    }

    @JsonIgnore
    public long getId() {
        return this.resource.getId();
    }

    @JsonIgnore
    public long getVersion() {
        return this.resource.getVersion();
    }

    @JsonProperty
    public ResourceIdentifier getResource() {
        return resource;
    }

    @JsonProperty
    public void setResource(ResourceIdentifier resourceIdentifier) {
        this.resource = resourceIdentifier;
    }

    @JsonProperty
    public String getDescription() {
        return description;
    }

    @JsonProperty
    public void setDescription(String description)
    {
        this.description = description;
    }

    @JsonIgnore
    public ResourceIdentifier resourceIdentifier()
    {
        return resource;
    }

    public UUID getTableName() {
        return tableName;
    }

    public void setTableName(UUID tableName) {
        this.tableName = tableName;
    }

    public Geometry getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(Geometry boundingBox) {
        this.boundingBox = boundingBox;
    }

    @Override
    public String toString()
    {
        return String.format(
            "CatalogResource [resource=%s, description=%s, key=%s, inputType=%s, resourceType=%s]",
            resource, description, key, inputType, resourceType);
    }
}
