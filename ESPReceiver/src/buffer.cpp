#include "buffer.h"
#include <cstring>
#include <Arduino.h>

Buffer::Buffer(unsigned int sizeIn) : size(sizeIn)
{
    data = (uint8_t*) malloc(sizeIn);
    endOffset = 0;
    beginOffset = 0;
    Serial.printf("Address : %#08x\n", (uint8_t*) malloc(sizeIn));
}

Buffer::~Buffer()
{
    free(data);
}

bool Buffer::putBunch(uint8_t* dataToAdd, int length){
    if(size-capacity < length)
        return false;
    int sizeLeftToEnd = size - endOffset;
    if(sizeLeftToEnd < length){
        memcpy((data + endOffset), dataToAdd, sizeLeftToEnd);
        memcpy(data, (dataToAdd + sizeLeftToEnd), length - sizeLeftToEnd);
        endOffset = length - sizeLeftToEnd;

    }else{
        memcpy((data+endOffset), dataToAdd, length);
        endOffset += length;
    }
    capacity += length;
    return true;
}

bool Buffer::popBunch(uint8_t* dataToExtract, int length){
    if(capacity < length)
        return false;
    int sizeLeftToEnd = size - beginOffset;
    if(sizeLeftToEnd < length){
        memcpy(dataToExtract, (data + endOffset), sizeLeftToEnd);
        memcpy((dataToExtract + sizeLeftToEnd), data, length - sizeLeftToEnd);
        beginOffset = length - sizeLeftToEnd;

    }else{
        memcpy(dataToExtract, (data + beginOffset), length);
        beginOffset += length;
    }
    capacity -= length;
    return true;
}

int Buffer::getCapacity(){
    return capacity;
}