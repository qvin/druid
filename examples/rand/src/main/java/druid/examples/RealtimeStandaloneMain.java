package druid.examples;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.metamx.common.lifecycle.Lifecycle;
import com.metamx.common.logger.Logger;
import com.metamx.druid.client.DataSegment;
import com.metamx.druid.client.ZKPhoneBook;
import com.metamx.druid.jackson.DefaultObjectMapper;
import com.metamx.druid.loading.DataSegmentPusher;
import com.metamx.druid.log.LogLevelAdjuster;
import com.metamx.druid.realtime.RealtimeNode;
import com.metamx.druid.realtime.SegmentAnnouncer;
import com.metamx.druid.realtime.SegmentPublisher;
import com.metamx.phonebook.PhoneBook;

import java.io.File;
import java.io.IOException;

/**
 * Standalone Demo Realtime process.
 * Created: 20121009T2050
 */
public class RealtimeStandaloneMain
{
  private static final Logger log = new Logger(RealtimeStandaloneMain.class);

  public static void main(String[] args) throws Exception
  {
    LogLevelAdjuster.register();

    Lifecycle lifecycle = new Lifecycle();

    RealtimeNode rn = RealtimeNode.builder().build();
    lifecycle.addManagedInstance(rn);

    // force standalone demo behavior (no zk, no db, no master, no broker)
    //
    // dummyPhoneBook will not be start()ed so it will not hang connecting to a nonexistent zk
    PhoneBook dummyPhoneBook = new ZKPhoneBook(new DefaultObjectMapper(), null, null) {
      @Override
      public boolean isStarted() { return true;}
    };

    rn.setPhoneBook(dummyPhoneBook);
    SegmentAnnouncer dummySegmentAnnouncer =
        new SegmentAnnouncer()
        {
          @Override
          public void announceSegment(DataSegment segment) throws IOException
          {
            // do nothing
          }

          @Override
          public void unannounceSegment(DataSegment segment) throws IOException
          {
            // do nothing
          }
        };
    SegmentPublisher dummySegmentPublisher =
        new SegmentPublisher()
        {
          @Override
          public void publishSegment(DataSegment segment) throws IOException
          {
            // do nothing
          }
        };

    // dummySegmentPublisher will not send updates to db because standalone demo has no db
    rn.setSegmentAnnouncer(dummySegmentAnnouncer);
    rn.setSegmentPublisher(dummySegmentPublisher);
    rn.setDataSegmentPusher(
        new DataSegmentPusher()
        {
          @Override
          public DataSegment push(File file, DataSegment segment) throws IOException
          {
            return segment;
          }
        }
    );

    rn.registerJacksonSubtype(new NamedType(RandomFirehoseFactory.class, "rand"));

    try {
      lifecycle.start();
    }
    catch (Throwable t) {
      log.info(t, "Throwable caught at startup, committing seppuku");
      t.printStackTrace();
      System.exit(2);
    }

    lifecycle.join();
  }
}