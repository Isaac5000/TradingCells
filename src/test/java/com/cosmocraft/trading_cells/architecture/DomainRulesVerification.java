package com.cosmocraft.trading_cells.architecture;

/** Runs feature-oriented domain contract verifiers without requiring a game server. */
public final class DomainRulesVerification {
    private DomainRulesVerification() {
    }

    public static void main(String[] args) { // NOSONAR - Required JVM entry point.
        TraderDomainVerification.verify();
        MachineDomainVerification.verify();
        BreederDomainVerification.verify();
        FarmerDomainVerification.verify();
        ConverterDomainVerification.verify();
        IronFarmDomainVerification.verify();
        MobFarmDomainVerification.verify();
        SkeletonFarmDomainVerification.verify();
        ZombieFarmDomainVerification.verify();
        QuarryDomainVerification.verify();
        CapturerDomainVerification.verify();
        ScreenLayoutDomainVerification.verify();
        InfusionExperienceDomainVerification.verify();
    }
}
