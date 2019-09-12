package custq;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 
 * Externalised configuration properties
 * @author regen
 *
 */
@ConfigurationProperties(prefix="queue")
public class QConfig {
	private Integer maxEntries;

	public Integer getMaxEntries() {
		return maxEntries;
	}

	public void setMaxEntries(Integer maxEntries) {
		this.maxEntries = maxEntries;
	}
}
