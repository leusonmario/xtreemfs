/*
 * Copyright (c) 2010-2012 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.util.List;

import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.rwre.RWReplicationStage.Operation;
import org.xtreemfs.osd.rwre.ReplicatedFileState.ReplicaState;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.AuthoritativeReplicaState;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ReplicaStatus;

/**
 * 
 * @author bjko
 */
public abstract class ReplicaUpdatePolicy {

    public static final long UNLIMITED_RESET = -1;

    public static final String  FILE_CELLID_PREFIX = "/file/";

    protected List<ServiceUUID> remoteOSDUUIDs;

    protected final ASCIIString cellId;
    
    protected long localObjVersion;

    protected final String localUUID;

    public ReplicaUpdatePolicy(List<ServiceUUID> remoteOSDUUIDs, String fileId,
            String localUUID) {
        this.remoteOSDUUIDs = remoteOSDUUIDs;
        this.cellId = fileToCellId(fileId);
        this.localUUID = localUUID;
        localObjVersion = -1;
    }

    public static String cellToFileId(ASCIIString cellId) {
        return cellId.toString().substring(FILE_CELLID_PREFIX.length());
    }

    public static ASCIIString fileToCellId(String fileId) {
        return new ASCIIString(FILE_CELLID_PREFIX + fileId);
    }
    
    /**
     * Returns true if the xLocations contains more then one replica and the policy is coordinated.
     */
    public static boolean requiresCoordination(XLocations xloc) {
        return (xloc.getReplicaUpdatePolicy().length() > 0
                && xloc.getNumReplicas() > 1
                && (xloc.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARA) 
                    || xloc.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE)
                    || xloc.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ))
                );
    }

    public List<ServiceUUID> getRemoteOSDUUIDs() {
        return remoteOSDUUIDs;
    }

    public ASCIIString getCellId() {
        return cellId;
    }

    public void objectFetched(long objVersion) {
        if (objVersion > localObjVersion)
            localObjVersion = objVersion;
    }

    public void setLocalObjectVersion(long localMaxObjVer) {
        localObjVersion = localMaxObjVer;
    }

    /**
     * 
     * @return true, if this is primary/backup, false if all replicas are
     *         primary
     */
    public abstract boolean requiresLease();

    /**
     * called to execute a reset
     * 
     * @param credentials
     * @param maxLocalOV
     */
    public abstract void executeReset(
        FileCredentials credentials,
        ReplicaStatus localReplicaState,
        ExecuteResetCallback callback);

    public static interface ExecuteResetCallback {
        public void finished(AuthoritativeReplicaState authState);

        public void failed(ErrorResponse error);
    }

    /**
     * called to execute a client write operation
     */
    public abstract void executeWrite(
        FileCredentials credentials,
        long objNo,
        long objVersion,
        InternalObjectData data,
        ClientOperationCallback callback);

    public abstract void executeTruncate(
        FileCredentials credentials,
        long newFileSize,
        long newObjectVersion,
        ClientOperationCallback callback);

    public static interface ClientOperationCallback {
        public void finished();
        public void failed(ErrorResponse error);
    }

    public abstract long onClientOperation(
        Operation operation,
        long objVersion,
        ReplicaState currentState,
        Flease lease) throws RedirectToMasterException, IOException;

    public abstract boolean onRemoteUpdate(long objVersion, ReplicaState currentState)
        throws IOException;

    /**
     * Checks if a remote update (write, truncate) can be accepted and applied.
     * 
     * @param objVersion
     *            the version of the remote update.
     * @return true, if the update can be accepted, false if it must be ignored.
     */
    public abstract boolean acceptRemoteUpdate(long objVersion) throws IOException;

    /**
     * @return true, if the policy needs to reset the replica
     * @param masterEpoch
     * @throws IOException
     */
    public abstract boolean onPrimary(int masterEpoch) throws IOException;

    /**
     * 
     * @return true, if the policy needs to reset the replica
     * @throws IOException
     */
    public abstract boolean onBackup() throws IOException;

    public abstract void onFailed() throws IOException;

    public abstract void closeFile();

}
