package com.solutions;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;


public class APIRateLimitSolution
{

    public static void main(String[] args) throws Exception
    {


        APIRateLimiter apiRateLimiter = new APIRateLimiter("Murli", LicenseType.LOW);
        Timer timer = new Timer("APIRateLimiter Timer");
        timer.scheduleAtFixedRate(new APiLimitResetTask(apiRateLimiter), 10L, apiRateLimiter.getApiRequestLimitInMillSec());
        //add request to queue
        apiRateLimiter.addRequest("Request 1");
        apiRateLimiter.addRequest("Request 2");

        Timer apiRequestProcessingTimer = new Timer("Api Request Processing Timer");
        apiRequestProcessingTimer.scheduleAtFixedRate(new ApiProcessingTask(apiRateLimiter),0L, 1L); // run after every 1 millis
        Timer waitingQueueTimer = new Timer("Waiting Queue Timer");
        waitingQueueTimer.scheduleAtFixedRate(new WaitingQueueTask(apiRateLimiter.getRequestList(), 20*1000L), 0L, 10L); // run clearing queue task after every 10 millisec
    }

}

/**
 * API Rate limiter which blocks the request to API from client
 * if it crosses the limit pre-defined in clients {@link LicenseType} licenseType
 */

class APIRateLimiter

{
    private final String clientName;
    private final LicenseType licenseType;
    //client api request list
    private final List<Request> requestList = new Vector<>();
    private int requestCount=0;

    public APIRateLimiter(String clientName, LicenseType licenseType)
    {
        this.clientName = clientName;
        this.licenseType = licenseType;
    }

    public String getClientName()
    {
        return clientName;
    }

    /**
     * Stub method for processing API request
     */
    private synchronized void processRequest(Request request)
    {
        if(licenseType.apiRequestAllowed(requestCount))
        {
            System.out.println("Processing client request "+ request.getId());
            requestList.remove(request);
            //Pass the request to actual API
            requestCount++;

        }
        else
        {
            //TODO should be replaced with checked exception later
            throw new RuntimeException("You have exceed the API request limit:"+licenseType.getApiRequestLimitInSec());
        }

    }

    /**
     * Process API requests in waiting queue.
     */
    public synchronized void processRequestsInQueue()
    {
        for (Request request:requestList)
        {
            processRequest(request);
        }
    }

    /**
     * Resets the request count, should be called after specific
     * interval of time
     */
    public synchronized void resetRequestCount()
    {
        requestCount=0;
    }

    public long getApiRequestLimitInMillSec()
    {
        return licenseType.getApiRequestLimitInSec() * 1000L;
    }

    public void addRequest(String requestId)
    {
        System.out.println("Adding Request"+ requestId+ "to waiting queue:");
        requestList.add(new Request(requestId,new Date().getTime()));
    }

    public List<Request> getRequestList()
    {
        return requestList;
    }
}

class Request
{
    private final String id;
    private final Long submissionTimeInMillis;

    public Request(String id, Long submissionTimeInMillis)
    {
        this.id = id;
        this.submissionTimeInMillis = submissionTimeInMillis;
    }

    public String getId()
    {
        return id;
    }

    public Long getSubmissionTimeInMillis()
    {
        return submissionTimeInMillis;
    }


}
enum LicenseType

{
    LOW(10),
    MEDIUM(20),
    HIGH(50);


    private final int apiRequestLimitInSec;

    LicenseType(int apiRequestLimitInSec)
    {
        this.apiRequestLimitInSec = apiRequestLimitInSec;
    }

    public int getApiRequestLimitInSec()
    {
        return apiRequestLimitInSec;
    }

    public boolean apiRequestAllowed(int requestCount)
    {
        return requestCount <= getApiRequestLimitInSec();
    }
}

class APiLimitResetTask extends TimerTask

{
    private final APIRateLimiter apiRateLimiter;

    public APiLimitResetTask(APIRateLimiter apiRateLimiter)
    {
        this.apiRateLimiter = apiRateLimiter;
    }

    @Override
    public void run()
    {
        System.out.println("API Rate limit for client "+ apiRateLimiter.getClientName() + " reset on "+ new Date());
        apiRateLimiter.resetRequestCount();
    }
}

class WaitingQueueTask extends TimerTask
{

    private final List<Request> requestList;
    private final long timeOutInMills;

    public WaitingQueueTask(List<Request> requestList, long timeOutInMills)
    {
        this.requestList = requestList;
        this.timeOutInMills = timeOutInMills;
    }

    @Override
    public void run()
    {

        for (Request request: requestList)
        {
            long currentTime = new Date().getTime();
            if(currentTime-request.getSubmissionTimeInMillis() > timeOutInMills)
            {
                requestList.remove(request);
            }

        }
    }
}

class ApiProcessingTask extends TimerTask

{
    private final APIRateLimiter apiRateLimiter;

    public ApiProcessingTask(APIRateLimiter apiRateLimiter)
    {
        this.apiRateLimiter = apiRateLimiter;
    }

    @Override
    public void run()
    {
        apiRateLimiter.processRequestsInQueue();
    }
}


