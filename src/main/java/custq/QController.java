package custq;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 
 * @author regen
 *
 *         curl -i -X POST -d @cc.json -H "Content-Type: application/json"
 *         http://localhost:8080/queueManager/addCompany?timeout=xxx curl -i -X
 *         GET -H "Content-Type: application/json"
 *         http://localhost:8080/queueManager/getCompany?timeout=xxx curl -i -X
 *         GET -H "Content-Type: application/json"
 *         http://localhost:8080/queueManager/getCompanies/3 rm
 *         ~/temp/data/q.mv.db
 */
@RestController
@RequestMapping("/queueManager")
public class QController {

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private QService qservice;

	@RequestMapping(value = "/addCompany", consumes = "application/json", method = RequestMethod.POST)
	public Long add(@RequestBody Company company, @RequestParam(defaultValue = "0") String timeout) {

		Long to = null;
		try {
			to = Long.decode(timeout);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid timeout supplied", e);
		}

		try {
			qservice.add(company, to);
		} catch (QueueFullException qfe) {
			throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT, "Entry cannot be added to queue at this time",
					qfe);
		}

		return company.getId();
	}

	@RequestMapping(value = "/getCompany", consumes = "application/json", method = RequestMethod.GET)
	public Company get(@RequestParam(defaultValue = "0") String timeout) {
		Long to = null;
		try {
			to = Long.decode(timeout);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid timeout supplied", e);
		}

		Company c = null;
		try {
			c = qservice.get(to);
		} catch (QueueEmptyException qfe) {
			throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT,
					"Entries cannot be received from queue at this time", qfe);
		}

		return c;
	}

	@RequestMapping(value = "/getCompanies/{size}", consumes = "application/json", method = RequestMethod.GET)
	public List<Company> getMany(@PathVariable int size) {
		List<Company> cl = qservice.getGroup(size);
		return cl;
	}

	@PostConstruct
	public void handleContextRefreshEvent() throws Exception {
		qservice.synchronise();
	}
}