/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package custq;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private QService qservice;

	/**
	 * Ensure that a new single company can be added to the queue
	 * @throws Exception
	 */
	@Test
	public void t1AddCompany() throws Exception {
		this.mockMvc
				.perform(post("/queueManager/addCompany").param("timeout", "5").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"GavWebCo\",\"description\":\"The final description\"}"))
				.andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$").isNumber());

	}

	/**
	 * Ensure that a single company can be retrieved from the queue
	 * @throws Exception
	 */
	@Test
	public void t2GetCompany() throws Exception {
		this.mockMvc
				.perform(get("/queueManager/getCompany").param("timeout", "5").contentType(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("GavWebCo"))
				.andExpect(jsonPath("$.description").exists());
	}

	/**
	 * Test with multiple threads (10 posts/10 gets with timeouts) to ensure that final queue length is zero
	 * @throws Exception
	 */
	@Test
	public void t3ValidateConcurrency() throws Exception {
		qservice.purge();

		CountDownLatch latch = new CountDownLatch(20);

		TestThread[] adjusters = new TestThread[] { 
				new AdderThread(mockMvc, latch),
				new AdderThread(mockMvc, latch),
				new AdderThread(mockMvc, latch),
				new AdderThread(mockMvc, latch),
				new AdderThread(mockMvc, latch),
				new GetterThread(mockMvc, latch),
				new AdderThread(mockMvc, latch),
				new AdderThread(mockMvc, latch),
				new GetterThread(mockMvc, latch),
				new GetterThread(mockMvc, latch),
				new AdderThread(mockMvc, latch),
				new AdderThread(mockMvc, latch),
				new GetterThread(mockMvc, latch),
				new GetterThread(mockMvc, latch),
				new GetterThread(mockMvc, latch),
				new AdderThread(mockMvc, latch),
				new GetterThread(mockMvc, latch),
				new GetterThread(mockMvc, latch),
				new GetterThread(mockMvc, latch),
				new GetterThread(mockMvc, latch)
				};

		List<TestThread> adjusterList = Arrays.asList(adjusters);

		adjusterList.stream().parallel().forEach(Thread::start);
		waitForChildThreadsToComplete(latch);

		assertTrue("Queue hasn't been reduced to zero",
				adjusterList.stream().map(a -> a.adjustedVal()).reduce(0,(r,n) -> r + n) == 0);

	}

	/**
	 * Ensure that a group can be correctly retrieved
	 * @throws Exception
	 */
	@Test
	public void t4GetAGroup() throws Exception {
		qservice.purge();
		
		this.mockMvc
		.perform(post("/queueManager/addCompany").param("timeout", "5").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"GavWebCo1\",\"description\":\"The final description\"}"))
		.andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$").isNumber());
		
		this.mockMvc
		.perform(post("/queueManager/addCompany").param("timeout", "5").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"GavWebCo2\",\"description\":\"The final description\"}"))
		.andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$").isNumber());

		this.mockMvc
		.perform(post("/queueManager/addCompany").param("timeout", "5").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"GavWebCo3\",\"description\":\"The final description\"}"))
		.andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$").isNumber());

		this.mockMvc
		.perform(post("/queueManager/addCompany").param("timeout", "5").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"GavWebCo4\",\"description\":\"The final description\"}"))
		.andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$").isNumber());

		this.mockMvc
		.perform(post("/queueManager/addCompany").param("timeout", "5").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"GavWebCo5\",\"description\":\"The final description\"}"))
		.andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$").isNumber());

		this.mockMvc
		.perform(get("/queueManager/getCompanies/4").contentType(MediaType.APPLICATION_JSON))
		.andDo(print()).andExpect(status().isOk())
		.andExpect(jsonPath("$",hasSize(4)))
		.andExpect(jsonPath("$.[0].name").value("GavWebCo1"))
		.andExpect(jsonPath("$.[1].name").value("GavWebCo2"))
		.andExpect(jsonPath("$.[2].name").value("GavWebCo3"))
		.andExpect(jsonPath("$.[3].name").value("GavWebCo4"));
		
	}

	/**
	 * Convenience method to hang around for latch to drop to complete
	 * @param latch
	 */
	private static void waitForChildThreadsToComplete(final CountDownLatch latch) {
        try {
            latch.await();
            System.out.println("All child threads have completed.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
	}

	/**
	 * root interface for test threads
	 * @author regen
	 *
	 */
	private static abstract class TestThread extends Thread {
		public abstract int adjustedVal();
	}
	
	/**
	 * Thread class to add company with a timeout wait of 2 seconds
	 * @author regen
	 *
	 */
	private static class AdderThread extends TestThread {

		private int added = 0;
		
		private MockMvc mockMvc;
		private CountDownLatch latch;

		public AdderThread(MockMvc mockMvc, CountDownLatch latch) {
			this.latch = latch;
			this.mockMvc = mockMvc;
		}
		
		@Override
		public void run() {
			try {
				this.mockMvc
						.perform(post("/queueManager/addCompany").param("timeout", "2")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"name\":\"GavWebCo\",\"description\":\"The final description\"}"))
						.andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$").isNumber());
				this.added = 1;
			} catch (Throwable e) {
			}
			
			latch.countDown();

		}

		public int adjustedVal() {
			return this.added;
		}

	}

	/**
	 * Thread class to fetch company with a timeout wait of 5 seconds
	 * @author regen
	 *
	 */
	private static class GetterThread extends TestThread {

		private int removed = 0;
		
		private MockMvc mockMvc;
		private CountDownLatch latch;

		public GetterThread(MockMvc mockMvc, CountDownLatch latch) {
			this.latch = latch;
			this.mockMvc = mockMvc;
		}
		
		@Override
		public void run() {
			try {
				this.mockMvc
				.perform(get("/queueManager/getCompany").param("timeout", "5").contentType(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("GavWebCo"))
				.andExpect(jsonPath("$.description").exists());
				this.removed = -1;
			} catch (Throwable e) {
			}
			
			latch.countDown();

		}

		public int adjustedVal() {
			return this.removed;
		}
	}

}
