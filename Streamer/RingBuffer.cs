using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Streamer
{
    public class RingBuffer
    {
        int beginOffset = 0;
        int endOffset = 0;
        int size = 0;
        int capacity = 0;
        byte[] buffer;

        public RingBuffer(int size)
        {
            this.size = size;
            buffer = new byte[size];
        }

        public bool putBunch(byte[] dataToAdd, int length)
        {
            if (size - capacity < length)
                return false;
            int sizeLeftToEnd = size - endOffset;
            if (sizeLeftToEnd < length)
            {
                Buffer.BlockCopy(dataToAdd, 0, buffer, endOffset, sizeLeftToEnd);
                Buffer.BlockCopy(dataToAdd, sizeLeftToEnd, buffer, 0, length - sizeLeftToEnd);
                endOffset = length - sizeLeftToEnd;

            }
            else
            {
                Buffer.BlockCopy(dataToAdd, 0, buffer , endOffset, length);
                endOffset += length;
            }
            capacity += length;
            return true;
        }

        public bool popBunch(out byte[] dataToExtract, int length)
         {
            dataToExtract = new byte[length];
            if (capacity < length)
                return false;
            int sizeLeftToEnd = size - beginOffset;
            if (sizeLeftToEnd < length)
            {
                Buffer.BlockCopy(buffer, endOffset, dataToExtract, 0, sizeLeftToEnd);
                Buffer.BlockCopy(buffer, 0, dataToExtract, sizeLeftToEnd, length - sizeLeftToEnd);
                beginOffset = length - sizeLeftToEnd;

            }
            else
            {
                Buffer.BlockCopy(buffer, beginOffset, dataToExtract, 0, length);
                beginOffset += length;
            }
            capacity -= length;
            return true;
        }

        public int getCapacity()
        {
            return capacity;
        }


    }
}
