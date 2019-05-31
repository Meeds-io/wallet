<script>
export default {
  extends: VueChart.Line,
  props: {
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    periodicity: {
      type: String,
      default: function() {
        return null;
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return null;
      },
    },
  },
  data () {
    return {
      incomeGradient: null,
      outcomeGradient: null
    }
  },
  mounted () {
    if (!this.walletAddress || !this.contractDetails || !this.contractDetails.address) {
      console.debug('No default contract is selected yet', this.walletaddress, this.contractDetails);
      return;
    }
    this.incomeGradient = this.$refs.canvas.getContext('2d').createLinearGradient(0, 0, 0, 450)
    this.outcomeGradient = this.$refs.canvas.getContext('2d').createLinearGradient(0, 0, 0, 450)

    this.incomeGradient.addColorStop(0, 'rgba(255, 0,0, 0.5)')
    this.incomeGradient.addColorStop(0.5, 'rgba(255, 0, 0, 0.25)');
    this.incomeGradient.addColorStop(1, 'rgba(255, 0, 0, 0)');
    
    this.outcomeGradient.addColorStop(0, 'rgba(0, 231, 255, 0.9)')
    this.outcomeGradient.addColorStop(0.5, 'rgba(0, 231, 255, 0.25)');
    this.outcomeGradient.addColorStop(1, 'rgba(0, 231, 255, 0)');

    this.transactionUtils.getTransactionsAmounts(this.contractDetails.networkId ,this.contractDetails.address, this.walletAddress, this.periodicity)
      .then((resp) => {
        return resp && resp.ok && resp.json();
      })
      .then((transactionsData) => {
        this.renderChart({
          labels: transactionsData.labels,
          datasets: [
            {
              label: 'Income',
              borderColor: '#FC2525',
              pointBackgroundColor: 'white',
              pointBorderColor: 'white',
              borderWidth: 1,
              backgroundColor: this.incomeGradient,
              data: transactionsData.income,
            },{
              label: 'Outcome',
              borderColor: '#05CBE1',
              pointBackgroundColor: 'white',
              pointBorderColor: 'white',
              borderWidth: 1,
              backgroundColor: this.outcomeGradient,
              data: transactionsData.outcome,
            }
          ]
        }, {responsive: true, maintainAspectRatio: false});
      })
      .catch(e => {
        this.$emit('error', e);
      });
  }
}
</script>
