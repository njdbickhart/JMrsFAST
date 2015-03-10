#include <stdlib.h>
#include <stdio.h>
#include "Common.h"
#include "MrsFAST.h"
#include "HashTable.h"
#include "Reads.h"
#include "Sort.h"
#include "RefGenome.h"

//Header methods that Pierre Lindenbaum used in his JNI BWA implementation

#define QUALIFIEDMETHOD(fun) Java_implementation_##fun
#define PACKAGEPATH "implementation/"

#define WHERE fprintf(stderr,"[%s][%d]\n",__FILE__,__LINE__)

#define VERIFY_NOT_NULL(a) if((a)==0) do {fprintf(stderr,"Throwing error from %s %d\n",__FILE__,__LINE__); throwIOException(env,"Method returned Null");} while(0)

#define CAST_REF_OBJECT(retType,method,ref) \
static retType _get##method(JNIEnv * env, jobject self)\
	{\
	jlong ptr;jfieldID field;\
	jclass c= (*env)->GetObjectClass(env,(self));\
	VERIFY_NOT_NULL(c);\
	field = (*env)->GetFieldID(env,c, ref, "J");\
	VERIFY_NOT_NULL(field);\
	ptr= (*env)->GetLongField(env,self,field);\
	return (retType)ptr;\
	}\
static void _set##method(JNIEnv *env, jobject self,retType value)\
	{\
	jfieldID field;\
	jclass c= (*env)->GetObjectClass(env,(self));\
	VERIFY_NOT_NULL(c);\
	field = (*env)->GetFieldID(env,c, ref, "J");\
	VERIFY_NOT_NULL(field);\
	(*env)->SetLongField(env,self, field,(jlong)value);\
	}	

static void throwIOException(JNIEnv *env,const char* msg)
	{
	jclass newExcCls = (*env)->FindClass(env,"java/io/IOException");
	(*env)->ThrowNew(env, newExcCls, msg);
	}

// Defining native methods 
/*********************************************************/

// Loading mrsfast index
CAST_REF_OBJECT(IHashTable*, MrsFastIndex, )