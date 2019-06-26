package custq;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QService {

	@Autowired
	private CompanyRepository companyRepository;
	
	private BlockingQueue<Company>  companyQueue = new ArrayBlockingQueue<>(5);;

	public void purge() {
		companyQueue.clear();
	}

	public void add(final Company c) throws QueueFullException {
		this.add(c, null);
	}

	public void add(final Company c, final Long timeout) throws QueueFullException {
		boolean added = false;
		try {
			added = companyQueue.offer(c, timeout == null ? 0 : timeout, TimeUnit.SECONDS);
			companyRepository.save(c);
		} catch (Exception e) {
			added = false;
		}

		if (!added) {
			throw new QueueFullException();
		}
	}

	public Company get() throws QueueEmptyException {
		return this.get(null);
	}

	public Company get(final Long timeout) throws QueueEmptyException {
		Company got = null;

		try {
			got = companyQueue.poll(timeout == null ? 0 : timeout, TimeUnit.SECONDS);
			companyRepository.delete(got);
		} catch (Exception e) {
		}

		if (got == null) {
			throw new QueueEmptyException();
		}

		return got;
	}

	public List<Company> getGroup(final int sz)  {
		if (sz < 1)
			throw new IllegalArgumentException("A group size must be greater than 1");

		List<Company> companies = new ArrayList<>(sz);

		if (companyQueue.drainTo(companies, sz) > 0) {
			companyRepository.deleteAll(companies);
		}
		
		return companies;
	}
	
	public void synchronise() throws Exception {
		this.purge();
		Iterable<Company> companies = this.companyRepository.findAll();
		
		for (Company c:companies) {
			companyQueue.offer(c);
			System.out.println("Recovered " + c.getName());
		}
	}
}
