package custq;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class StandaloneQTest {
	// Mocking the repo itself
	@Mock
	CompanyRepository companyRepositoryMock;

	//Inject the repo into the QService instance
	@InjectMocks
	QService qservice = new QService();

	/**
	 * Ensure that the repository interactions are correct when a company is added and then removed
	 * through the service. 
	 */
	@Test
	public void addEntry() {
		qservice.purge();
		Company c = new Company(10, "TestCo", "TestCo Description","07423134565");
		ArgumentCaptor<Company> saveCaptor = ArgumentCaptor.forClass(Company.class);
		ArgumentCaptor<Company> delCaptor = ArgumentCaptor.forClass(Company.class);

		try {
			qservice.add(c);
		} catch (Exception e) {
			fail("Entry cannot be added to queue - " + e);
		}

		verify(companyRepositoryMock).save(saveCaptor.capture());
		assertTrue("Correct customer was not persisted to database", saveCaptor.getValue().getId() == 10);

		try {
			c = qservice.get();
		} catch (Exception e) {
			c = null;
		}
		verify(companyRepositoryMock).delete(delCaptor.capture());

		assertTrue("Customer not added to queue", c != null);
		assertTrue("Customer was not removed from database", delCaptor.getValue().getId() == 10);
	}

	/**
	 * Ensure that the queue full exception is thrown when an 'add entry' attempt is made to a full queue 
	 */
	@Test(expected = QueueFullException.class)
	public void fullQueue() throws QueueFullException {
		fillQueue();

		qservice.add(new Company());
	}

	/**
	 * Ensure that the queue empty exception is thrown when an 'get entry' attempt is made to an empty queue 
	 */
	@Test(expected = QueueEmptyException.class)
	public void emptyQueue() throws QueueEmptyException {
		qservice.purge();
		Company[] companies = new Company[] { new Company(10, "TestCo", "TestCo Description","07423134565"),
				new Company(11, "TestCo", "TestCo Description","07423134565") };

		for (Company c : companies) {
			try {
				qservice.add(c);
			} catch (Exception e) {
				fail("Company could not be added - " + e);
			}
		}

		verify(companyRepositoryMock, times(2)).save(isA(Company.class));

		for (int i : new int[] { 0, 1 }) {
			try {
				qservice.get();
			} catch (Exception e) {
				fail("Company could not be removed - " + e);
			}

		}

		verify(companyRepositoryMock, times(2)).delete(isA(Company.class));

		qservice.get();
	}

	/**
	 * Ensure entry can be added following its collection during timeout period
	 * @throws QueueFullException
	 */
	@Test
	public void addEntryWithTimeout() throws QueueFullException {
		fillQueue();

		Runnable remover = new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(2000);
					qservice.get();
				} catch (Exception e) {
					fail("Cannot remove company from queue");
				}
			}
		};

		Thread removerThread = new Thread(remover);
		removerThread.start();

		qservice.add(new Company(50, "NewCo", "TestCo Description","07423134565"), 3L);
	}

	/*
	 * Ensure entry can be collected following its addition during timeout period
	 * @throws QueueFullException
	 */
	@Test
	public void removeEntryWithTimeout() throws QueueEmptyException {
		qservice.purge();

		Runnable adder = new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(2000);
					qservice.add(new Company(50, "NewCo", "TestCo Description","07423134565"));
				} catch (Exception e) {
					fail("Cannot add company to queue");
				}
			}
		};

		Thread adderThread = new Thread(adder);
		adderThread.start();

		qservice.get(3L);
	}
	
	/**
	 * Ensure that a group can be correctly received.
	 * @throws Exception
	 */
	@Test
	public void getGroup() throws Exception {
		qservice.purge();
		
		qservice.getGroup(4);
		verify(companyRepositoryMock, times(0)).deleteAll(any());
		
		//If it gets here then no exception should have been thrown - good
		
		fillQueue();
		
		List<Company> companies = qservice.getGroup(4);
		
		verify(companyRepositoryMock, times(1)).deleteAll(any());
		assertTrue("Companies not correctly removed en-masse",
				companies.stream().map(c -> c.getId()).reduce(0L,(r,n)->r+n) == 46);
	}
	
	/**
	 * Test to ensure that queue synchronisation is correctly achieved
	 * @throws Exception
	 */
	@Test
	public void syncQ() throws Exception {
		qservice.purge();
		
		Company[] rc = new Company[] { new Company(10, "TestCo", "TestCo Description","07423134565"),
				new Company(11, "TestCo", "TestCo Description","07423134565"), new Company(12, "TestCo", "TestCo Description","07423134565"),
				new Company(13, "TestCo", "TestCo Description","07423134565")};
		List<Company> retComps = Arrays.asList(rc);
		
		when(companyRepositoryMock.findAll()).thenReturn(retComps);
		
		qservice.synchronise();
		
		List<Company> companies = qservice.getGroup(4);
		
		assertTrue("Synchronise hasn't completed fully", companies.size() == retComps.size());
		
		final Set<Long> dbComps = retComps.stream().map(Company::getId).collect(toSet());
		final Set<Long> outComps = companies.stream().map(Company::getId).collect(toSet());
		
		dbComps.removeAll(outComps);
		
		assertTrue("Invalid synchonisation of database to Q", dbComps.size() == 0);
	}
	
	/**
	 * Helper method to fill the queue with fun
	 */
	private void fillQueue() {
		qservice.purge();
		Company[] companies = new Company[] { new Company(10, "TestCo", "TestCo Description","07423134565"),
				new Company(11, "TestCo", "TestCo Description","07423134565"), new Company(12, "TestCo", "TestCo Description","07423134565"),
				new Company(13, "TestCo", "TestCo Description","07423134565"), new Company(14, "TestCo", "TestCo Description","07423134565") };

		for (Company c : companies) {
			try {
				qservice.add(c);
			} catch (Exception e) {
				fail("Company could not be added - " + e);
			}
		}

		verify(companyRepositoryMock, times(5)).save(isA(Company.class));
	}
}
