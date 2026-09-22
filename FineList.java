public class FineList 
{

    // Sentinel nodes: head holds the smallest possible value and tail the largest,
    // so every real value always sits between them and traversal never runs off the list.
    private final Node head;
    private final Node tail;

    public FineList() 
    {
        head = new Node(Integer.MIN_VALUE);
        tail = new Node(Integer.MAX_VALUE);

        head.next = tail;
    }

    public boolean add(int value) 
    {
        // Lock the first node before reading anything from it.
        head.lock.lock();
        Node pred = head;
        try
        {
            // Lock the second node so we hold two neighbouring locks (pred and curr).
            Node curr = pred.next;
            curr.lock.lock();
            try
            {
                // Hand-over-hand traversal: release the node behind us, step forward,
                // then lock the next node. We always hold at least one lock, and locks
                // are taken in the same head-to-tail order by every thread (no deadlock).
                while(curr.value < value)
                {
                    pred.lock.unlock();
                    pred = curr;
                    curr = curr.next;
                    curr.lock.lock();
                }

                // Value already present: sets don't allow duplicates.
                if(curr.value == value)
                {
                    return false;
                }

                // Both pred and curr are locked, so no other thread can change the link
                // between them while we insert the new node in sorted position.
                Node node = new Node(value);
                node.next = curr;
                pred.next = node;
                return true;
            }
            finally
            {
                // Always release curr's lock, whether we returned true or false.
                curr.lock.unlock();
            }
        }
        finally
        {
            // Always release pred's lock last.
            pred.lock.unlock();
        }
    }

    public boolean remove(int value) 
    {
        // Lock the first node before reading anything from it.
        head.lock.lock();
        Node pred = head;
        try
        {
            // Lock the second node so we hold two neighbouring locks (pred and curr).
            Node curr = pred.next;
            curr.lock.lock();
            try
            {
                // Hand-over-hand traversal: let go of the node behind us only after
                // we have locked the node in front.
                while(curr.value < value)
                {
                    pred.lock.unlock();
                    pred = curr;
                    curr = curr.next;
                    curr.lock.lock();
                }

                // Found it: unlink curr. Both pred and curr must be locked here,
                // otherwise a concurrent remove of pred or curr could undo this change.
                if(curr.value == value)
                {
                    pred.next = curr.next;
                    return true;
                }

                // Value not in the list.
                return false;
            }
            finally
            {
                // Always release curr's lock.
                curr.lock.unlock();
            }
        }
        finally
        {
            // Always release pred's lock.
            pred.lock.unlock();
        }
    }

    public boolean contains(int value) 
    {
        // Lock the first node before reading anything from it.
        head.lock.lock();
        Node pred = head;
        try
        {
            // Lock the second node.
            Node curr = pred.next;
            curr.lock.lock();
            try
            {
                // Same hand-over-hand traversal as add and remove, so the reader
                // never looks at a node that another thread is in the middle of changing.
                while(curr.value < value)
                {
                    pred.lock.unlock();
                    pred = curr;
                    curr = curr.next;
                    curr.lock.lock();
                }

                // curr is the first node with value >= target; check for an exact match.
                return curr.value == value;
            }
            finally
            {
                // Always release curr's lock.
                curr.lock.unlock();
            }
        }
        finally
        {
            // Always release pred's lock.
            pred.lock.unlock();
        }
    }
}