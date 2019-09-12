package custq;

import org.springframework.data.repository.CrudRepository;

/**
 * Standard Spring Data repository to allow persistence of the queue information
 * @author regen
 *
 */
public interface CompanyRepository extends CrudRepository<Company, Long> {

}
